import json
import sys
import maprdb

# the subset of the data we are loading -- actual will be min(DATA_SIZE, table size)
#DATA_SIZE = 10000

# input json file for tenants details
DATA_TENANTS_PATH = "/opt/tenants/maprdb_python_onboarding/ws.json"

# input json file for tenants data details
DATA_DETAILS_PATH = "/opt/tenants/maprdb_python/maint.json"

# path in HDFS/MapR-FS where the table will live
TABLE_TENANTS_PATH = "/data/tenants/tenantdata"
TABLE_DETAILS_PATH = "/data/tenants/metadata"

def open_db():
    return (maprdb.connect())

# create or get existing table
def open_table(connection, p):
    print("opening table %s" % p)
    if connection.exists(p):
        print("deleting old table %s" % p)
        connection.delete(p)
    return (connection.create(p))

i = 0
print("opening JSON file %s" % DATA_TENANTS_PATH)
db = open_db()
t = open_table(db, TABLE_TENANTS_PATH)

# first load the tenant details
with open(DATA_TENANTS_PATH) as json_data:
    d = json.load(json_data)


    for doc in d:
        i += 1
        if (i > DATA_SIZE):
             break

        inner = doc["tenant_input"]
        newdoc = maprdb.Document(
            { '_id': str(doc["tenant_id"]),
              'tenant_input.country': str(inner["country"]),
              'tenant_input.unit': str(inner["unit"]),
              'tenant_input.description': str(inner["description"]),
              'tenant_input.case_stack':
                  str(inner["case_stack"]),
              'app_code': str(doc["app_code"]),

              'dev': str(doc["dev"]),
              'staging': str(doc["staging"]),
              'production': str(doc["production"]),
              'timestamp': str(doc["timestamp"]),
              'contact': str(doc["contact"]) } )
        t.insert_or_replace(newdoc)
print(" ...done")
i = 0

# now load the light metadata
# we assume that the actual metadata is referenced 
# from source of records or red metadata repo
t = open_table(db, TABLE_DETAILS_PATH)
print("opening JSON file %s" % DATA_DETAILS_PATH)
with open(DATA_DETAILS_PATH) as json_data:
    d = json.load(json_data)


    for doc in d:
        i += 1
        if (i > DATA_SIZE):
             break

        newdoc = maprdb.Document({
              '_id': str(doc["tenant_id"]),
              'app_code': str(doc["app_code"]),
              'data_format': str(doc["data_format"]),
              'timestamp': str(doc["timestamp"]),
              'contact': str(doc["contact"]),


              # setting an array
              'source_of_data': doc["source_of_data"],
              'modified': doc["modified"],

              'attributes_modified': str(doc["attributes_modified"]),
              'num_attributes': str(doc["num_attributes"]),
              'list_attributes': str(doc["list_attributes"])})
        t.insert_or_replace(newdoc)
print(" ...done")
