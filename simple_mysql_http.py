'''
Created on Oct 9, 2018

@author: verdyr
'''

import SimpleHTTPServer, SocketServer
import urlparse, os

import mysql.connector

PORT = 4000

class MyHandler(SimpleHTTPServer.SimpleHTTPRequestHandler):
    def do_OPTIONS(self):

        self.send_response(200, "OK")
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'X-Requested-With, Content-Type')

    def do_GET(self):

        # Parse query data to find out what was requested
        parsedParams = urlparse.urlparse(self.path)

        # "data" serves the dynamic content, the rest will go to the file system
        if (parsedParams.path == '/data.json'):
            params = urlparse.parse_qs(parsedParams.query)
	          tableFilter = params.get('table', ['test1'])[0]
            dataFilter = params.get('name', ["John", "Bob", "Alice", "Tim", "Sara", "Eugene"])[0]
            address = params.get('address', ["Denmark", "Finland", "Sweden"])[0];
            db = mysqldb.connect("locahost", "root", "12345", "testdatabase") 
            curs=db.cursor() 
            curs.execute("SELECT * from test1")
            res = curs.fetchall() 
            self.send_response(200)
	          self.send_header('Access-Control-Allow-Origin', '*')
            self.send_header('Content-Type', 'application/json')
            self.end_headers()           
            self.wfile.write(res)
        else:
            f = self.send_head()
            if f:
                self.copyfile(f, self.wfile)
                f.close()
            

if __name__ == '__main__':
    Handler = MyHandler

    httpd = SocketServer.TCPServer(("", PORT), Handler)

    print "Serving at port", PORT
    httpd.serve_forever()
