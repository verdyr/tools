#!/usr/bin/env python3

from http.server import HTTPServer, BaseHTTPRequestHandler
import os, logging

PORT = 4000

class MyHandler(BaseHTTPRequestHandler):
    def _set_response(self):
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'X-Requested-With, Content-Type')
        self.send_header('Content-Type', 'application/json')
        self.end_headers()

    def do_OPTIONS(self):
        self._set_response()

    def do_GET(self):
        logging.info("GET request,\nPath: %s\nHeaders:\n%s\n", str(self.path), str(self.headers))
        self._set_response()
        self.wfile.write("GET request for {}".format(self.path).encode('utf-8'))

    def do_POST(self):
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        logging.info("POST request,\nPath: %s\nHeaders:\n%s\n\nBody:\n%s\n", str(self.path), str(self.headers), post_data.decode('utf-8'))
        self._set_response()
        self.wfile.write("POST request for {}".format(self.path).encode('utf-8'))
        
#    def start()

if __name__ == '__main__':
    Handler = MyHandler
    logging.basicConfig(level=logging.INFO)
    httpd = HTTPServer(("", PORT), Handler)
    logging.info("Serving at port: %s\n", PORT)
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    logging.info("Stopping server ...\n")
