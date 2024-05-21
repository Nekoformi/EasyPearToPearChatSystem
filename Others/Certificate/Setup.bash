#!/bin/bash

echo "# Setup"

keyLength=4096
validityPeriod=36500
countryName="JP"
organizationName="Nekoformi"
organizationalUnitName="Easy Pear to Pear Chat System"

    echo "## Create root certificate and key store"

    openssl genrsa -out Root.key $keyLength

    openssl req -new -x509 \
        -key Root.key \
        -subj "/C=${countryName}/O=${organizationName}/OU=${organizationalUnitName}/CN=ROOT" \
        -days $validityPeriod \
        -out Root.crt

    keytool -import \
        -file Root.crt \
        -storepass "Hello root!" \
        -keystore DefaultRootKeyStore.jks

    echo "## Create server certificate"

        echo "### Create server certificate signing request"

        openssl genrsa -out Server.key $keyLength

        openssl req -new \
            -key Server.key \
            -subj "/C=${countryName}/O=${organizationName}/OU=${organizationalUnitName}/CN=SERVER" \
            -days $validityPeriod \
            -out Server.csr

        echo "### Have the root issue a server certificate"

        openssl x509 -req \
            -in Server.csr \
            -CA Root.crt \
            -CAkey Root.key \
            -CAcreateserial \
            -days $validityPeriod \
            -out Server.crt

        echo "### Pack the server certificate"

        openssl pkcs12 -export \
            -in Server.crt \
            -inkey Server.key \
            -certfile Root.crt \
            -passout "pass:Hello server!" \
            -out DefaultServerCertificate.p12

    echo "## Create client certificate"

        echo "### Create client certificate signing request"

        openssl genrsa -out Client.key $keyLength

        openssl req -new \
            -key Client.key \
            -subj "/C=${countryName}/O=${organizationName}/OU=${organizationalUnitName}/CN=CLIENT" \
            -days $validityPeriod \
            -out Client.csr

        echo "### Have the root issue a client certificate"

        openssl x509 -req \
            -in Client.csr \
            -CA Root.crt \
            -CAkey Root.key \
            -CAcreateserial \
            -days $validityPeriod \
            -out Client.crt

        echo "### Pack the client certificate"

        openssl pkcs12 -export \
            -in Client.crt \
            -inkey Client.key \
            -certfile Root.crt \
            -passout "pass:Hello client!" \
            -out DefaultClientCertificate.p12
