# generate valid_server_cert.pem with the following command:
#    openssl req -new -x509 -keyout valid_server_cert.pem -out valid_server_cert.pem -days 365 -nodes
# run as follows:
#    python simple-https-server.py
# then in your browser, visit:
#    https://localhost:4443

import BaseHTTPServer, SimpleHTTPServer
import ssl

httpd = BaseHTTPServer.HTTPServer(('localhost', 4443), SimpleHTTPServer.SimpleHTTPRequestHandler)
httpd.socket = ssl.wrap_socket (httpd.socket, certfile='./valid_server_cert.pem', server_side=True)
httpd.serve_forever()