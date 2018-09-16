

import scopt.OptionParser

import org.apache.spark.ml.linalg.Vector

import scala.reflect.runtime.universe._

import org.apache.spark.ml.Transformer
import org.apache.spark.ml.regression.LinearRegression
import org.apache.spark.mllib.evaluation.RegressionMetrics
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
  * A reworked example runner for linear regression with elastic-net (mixing L1/L2) regularization.
  *
  * A synthetic dataset can be found at `data/mllib/sample_linear_regression_data.txt` which can be
  * trained by
  * to run this use same parameters as in original example
  * If you use it as a template to create your own app, please use `spark-submit` to submit your app.
  */
object MLMigrationTest {

  case class Params(
                     input: String = null,
                     testInput: String = "",
                     dataFormat: String = "libsvm",
                     regParam: Double = 0.0,
                     elasticNetParam: Double = 0.0,
                     maxIter: Int = 100,
                     tol: Double = 1E-6,
                     fracTest: Double = 0.2) extends AbstractParams[Params]


  /** Load a dataset from the given path, using the given format */
  private def loadData(
                            spark: SparkSession,
                            path: String,
                            format: String,
                            expectedNumFeatures: Option[Int] = None): DataFrame = {
    import spark.implicits._

    format match {
      case "dense" => MLUtils.loadLabeledPoints(spark.sparkContext, path).toDF()
      case "libsvm" => expectedNumFeatures match {
        case Some(numFeatures) => spark.read.option("numFeatures", numFeatures.toString)
          .format("libsvm").load(path)
        case None => spark.read.format("libsvm").load(path)
      }
      case _ => throw new IllegalArgumentException(s"Bad data format: $format")
    }
  }


  /**
    * Load training and test data from files.
    * @param input  Path to input dataset.
    * @param dataFormat  "libsvm" or "dense"
    * @param testInput  Path to test dataset.
    * @param algo  Classification or Regression
    * @param fracTest  Fraction of input data to hold out for testing. Ignored if testInput given.
    * @return  (training dataset, test dataset)
    */
  private def loadDatasets(
                                input: String,
                                dataFormat: String,
                                testInput: String,
                                algo: String,
                                fracTest: Double): (DataFrame, DataFrame) = {
    val spark = SparkSession
      .builder
      .getOrCreate()

    // Load training data
    val origExamples: DataFrame = loadData(spark, input, dataFormat)

    // Load or create test set
    val dataframes: Array[DataFrame] = if (testInput != "") {
      // Load testInput.
      val numFeatures = origExamples.first().getAs[Vector](1).size
      val origTestExamples: DataFrame =
        loadData(spark, testInput, dataFormat, Some(numFeatures))
      Array(origExamples, origTestExamples)
    } else {
      // Split input into training, test.
      origExamples.randomSplit(Array(1.0 - fracTest, fracTest), seed = 12345)
    }

    val training = dataframes(0).cache()
    val test = dataframes(1).cache()

    val numTraining = training.count()
    val numTest = test.count()
    val numFeatures = training.select("features").first().getAs[Vector](0).size
    println("Loaded data:")
    println(s"  numTraining = $numTraining, numTest = $numTest")
    println(s"  numFeatures = $numFeatures")

    (training, test)
  }



  /**
    * Evaluate the given RegressionModel on data. Print the results.
    * @param model  Must fit RegressionModel abstraction
    * @param data  DataFrame with "prediction" and labelColName columns
    * @param labelColName  Name of the labelCol parameter for the model
    *
    * TODO: Change model type to RegressionModel once that API is public. SPARK-5995
    */
  private def evaluateRegressionModel(
                                       model: Transformer,
                                       data: DataFrame,
                                       labelColName: String): Unit = {
    val fullPredictions = model.transform(data).cache()
    val predictions = fullPredictions.select("prediction").rdd.map(_.getDouble(0))
    val labels = fullPredictions.select(labelColName).rdd.map(_.getDouble(0))
    val RMSE = new RegressionMetrics(predictions.zip(labels)).rootMeanSquaredError
    println(s"  Root mean squared error (RMSE): $RMSE")
  }



  def main(args: Array[String]) {
    val defaultParams = Params()

    val parser = new OptionParser[Params]("LinearRegressionExample") {
      head("LinearRegressionExample: an example Linear Regression with Elastic-Net app.")
      opt[Double]("regParam")
        .text(s"regularization parameter, default: ${defaultParams.regParam}")
        .action((x, c) => c.copy(regParam = x))
      opt[Double]("elasticNetParam")
        .text(s"ElasticNet mixing parameter. For alpha = 0, the penalty is an L2 penalty. " +
          s"For alpha = 1, it is an L1 penalty. For 0 < alpha < 1, the penalty is a combination of " +
          s"L1 and L2, default: ${defaultParams.elasticNetParam}")
        .action((x, c) => c.copy(elasticNetParam = x))
      opt[Int]("maxIter")
        .text(s"maximum number of iterations, default: ${defaultParams.maxIter}")
        .action((x, c) => c.copy(maxIter = x))
      opt[Double]("tol")
        .text(s"the convergence tolerance of iterations, Smaller value will lead " +
          s"to higher accuracy with the cost of more iterations, default: ${defaultParams.tol}")
        .action((x, c) => c.copy(tol = x))
      opt[Double]("fracTest")
        .text(s"fraction of data to hold out for testing. If given option testInput, " +
          s"this option is ignored. default: ${defaultParams.fracTest}")
        .action((x, c) => c.copy(fracTest = x))
      opt[String]("testInput")
        .text(s"input path to test dataset. If given, option fracTest is ignored." +
          s" default: ${defaultParams.testInput}")
        .action((x, c) => c.copy(testInput = x))
      opt[String]("dataFormat")
        .text("data format: libsvm (default), dense (deprecated in Spark v1.1)")
        .action((x, c) => c.copy(dataFormat = x))
      arg[String]("<input>")
        .text("input path to labeled examples")
        .required()
        .action((x, c) => c.copy(input = x))
      checkConfig { params =>
        if (params.fracTest < 0 || params.fracTest >= 1) {
          failure(s"fracTest ${params.fracTest} value incorrect; should be in [0,1).")
        } else {
          success
        }
      }
    }

    parser.parse(args, defaultParams) match {
      case Some(params) => run(params)
      case _ => sys.exit(1)
    }
  }



  def run(params: Params): Unit = {
    val spark = SparkSession
      .builder
      .appName(s"LinearRegressionExample with $params")
      .getOrCreate()

    println(s"LinearRegressionExample with parameters:\n$params")

    // Load training and test data and cache it.
    val (training: DataFrame, test: DataFrame) = loadDatasets(params.input,
      params.dataFormat, params.testInput, "regression", params.fracTest)

    val lir = new LinearRegression()
      .setFeaturesCol("features")
      .setLabelCol("label")
      .setRegParam(params.regParam)
      .setElasticNetParam(params.elasticNetParam)
      .setMaxIter(params.maxIter)
      .setTol(params.tol)

    // Train the model
    val startTime = System.nanoTime()
    val lirModel = lir.fit(training)
    val elapsedTime = (System.nanoTime() - startTime) / 1e9
    println(s"Training time: $elapsedTime seconds")

    // Print the weights and intercept for linear regression.
    println(s"Weights: ${lirModel.coefficients} Intercept: ${lirModel.intercept}")

    println("Training data results:")
    evaluateRegressionModel(lirModel, training, "label")
    println("Test data results:")
    evaluateRegressionModel(lirModel, test, "label")

    spark.stop()
  }
}
// scalastyle:on println


/**
  * Abstract class for parameter case classes.
  * This overrides the [[toString]] method to print all case class fields by name and value.
  * @tparam T  Concrete parameter class.
  */
abstract class AbstractParams[T: TypeTag] {

  private def tag: TypeTag[T] = typeTag[T]

  /**
    * Finds all case class fields in concrete class instance, and outputs them in JSON-style format:
    * {
    *   [field name]:\t[field value]\n
    *   [field name]:\t[field value]\n
    *   ...
    * }
    */
  override def toString: String = {
    val tpe = tag.tpe
    val allAccessors = tpe.decls.collect {
      case m: MethodSymbol if m.isCaseAccessor => m
    }
    val mirror = runtimeMirror(getClass.getClassLoader)
    val instanceMirror = mirror.reflect(this)
    allAccessors.map { f =>
      val paramName = f.name.toString
      val fieldMirror = instanceMirror.reflectField(f)
      val paramValue = fieldMirror.get
      s"  $paramName:\t$paramValue"
    }.mkString("{\n", ",\n", "\n}")
  }
}


