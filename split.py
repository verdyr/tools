chunksize = 9000000
fid = 1
with open('db2export') as infile:
    f = open('chunk%d.csv' %fid, 'w')
    for i,line in enumerate(infile):
        f.write(line)
        if not i%chunksize:
            f.close()
            fid += 1
            f = open('chunk%d.csv' %fid, 'w')
    f.close()

