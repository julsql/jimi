# JIMI API

![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Overview

JIMI is a school project aimed at developing an application for managment of time and calendar, which includes the functionality to open chat with a chatbot that will handle the database of the calendar.

This JIMI API is a Java-based API built using Spring Boot. The answer and NLP usage are using the OpenAI API. It allows users to perform various operations on a MariaBD database, facilitating calendar management functionalities. This README provides essential information on how to set up and use the project.

## Table of Contents

- [Local installation](#local-installation)
- [Deployment](#deployment)
- [SQL](#sql)
- [Usage](#usage)
- [Database](#database)
- [Structure](#structure)
- [License](#license)
- [Authors](#authors)

## Local installation

To get started with the JIMI Chatbot, follow these steps:

1. Clone the repository:
   ```shell
   git clone git@github.com:JIMIDevLab/jimi_api.git
   ```

2. Navigate to the project directory:

    ```shell
    cd jimi_api
   ```

3. Install the required dependencies (listed in the [pom.xml](pom.xml)) and run the tests:

    ```shell
    ./mvnw clean install
    ```
4. Configure your database connection and openAI key by creating the [application.yml](src/main/resources/application.yml) file.

    ```yaml
   # Specifies server port
   server:
      port: 8080
      servlet:
          encoding:
              charset: UTF-8
              force-response: true
              enabled: true
   
   spring:
      datasource:
        url: jdbc:mariadb://SERVER_IP:3306/DATABASE?useSSL=false&serverTimezone=America/New_York
        username: username
        password: password
        driver-class-name: org.mariadb.jdbc.Driver
        defer-datasource-initialization: true
      sql:
        init:
        mode: never
   
   # Swagger setting to visualize JSON format in doc page.
   springdoc:
     default-produces-media-type: application/json
   
   openai:
     model: gpt-3.5-turbo
     api:
       url: https://api.openai.com/v1/chat/completions
       key: api-key
   
   logging:
     level:
       org:
         springframework:
           jdbc:
             core: DEBUG
           web: info
       root: info
     file:
       path: ./logs
   ```
   
5. Build and run the application:

    ```shell
    ./mvnw spring-boot:run
    ```

6. Access the API documentation using Swagger UI at
   http://localhost:8080/swagger-ui.html.

   > You can also create a SpringBoot configuration on IntelliJ by running the file [JimiApiApplication.java](src/main/java/com/tsp/jimi_api/JimiApiApplication.java)

## Deployment

Configure the VM

   ```shell
   sudo apt-get update
   sudo apt-get install apache2
   sudo apt-get install git
   sudo apt-get install openjdk-17-jdk
   ```

Add PATH:

   ```bash
   nano ~/.bashrc
   # export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
   source ~/.bashrc
   ```

Then clone the project

   ```shell
   git clone git@github.com:JIMIDevLab/jimi_api.git
   cd jimi_api
   ```

You need to generate the .jar file to run in the VM.

   ```shell
   ./mvnw clean install
   ```
> Don't forget to add the application.yml file with your configuration as written in [Local installation](#local-installation)

Then you launch the jar file in the server:
   ```shell
   nohup java -jar target/jimi-api.jar &
   ```
    
Add the Apache configuration ```/etc/apache2/sites-available/spring-config.conf```

   ```txt
   <VirtualHost *:80>
   ServerName url.domain.com
   ServerAdmin admin@email.fr
   
       ProxyPass / http://localhost:8080/
       ProxyPassReverse / http://localhost:8080/
   
       ErrorLog ${APACHE_LOG_DIR}/error.log
       CustomLog ${APACHE_LOG_DIR}/access.log combined
   </VirtualHost>
   ```

Run the configuration

   ```shell
   sudo a2ensite spring-config.conf
   sudo a2enmod proxy
   sudo a2enmod proxy_http
   sudo a2enmod proxy_html
   sudo systemctl restart apache2
   ```

To kill the processus, run
   ```shell
   ps aux | grep jimi-api.jar
   kill 12345
   ```

## SQL

Configure MariaDB on a server
```bash
sudo apt update
sudo apt install mariadb-server
sudo systemctl status mariadb
sudo mysql_secure_installation
sudo mysql -u root -p

sudo nano /etc/mysql/mariadb.conf.d/50-server.cnf
# bind-address = 0.0.0.0
sudo systemctl restart mariadb
```

```mysql
CREATE USER 'username'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON *.* TO 'username'@'localhost' WITH GRANT OPTION;
CREATE USER 'username'@'%' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON *.* TO 'username'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;
```

Create the database Jimi: connect to maria `sudo mysql -u root -p` and execute:
```sql
CREATE DATABASE JIMI
USE JIMI
```

Then create the table with the sql code [create.sql](src/main/resources/create.sql)

## Usage
The JIMI API handles requests made by the [JIMI Flutter App](https://github.com/JIMIDevLab/jimi_app) to interact with the database.

For detailed API documentation and usage examples, refer to the Swagger Documentation when running the application locally or on the deployed API http://jimi-api.h.minet.net/swagger-ui/index.html#/.

## ChatGPT

The request sent to the OpenAI API must be a JSON format like this:
```json
{
   "model":"gpt-3.5-turbo",
   "messages":[
      {
         "role":"system",
         "content":"You are a helpful assistant."
      },
      {
         "role":"user",
         "content":"Quelle est la capitale de la France ?"
      }
   ]
}
```

## Database
To manipulate the database, you can run the sql file:

- [create.sql](src/main/resources/create.sql) to drop and create the structure.

## Structure

- [configurations/](src/main/java/com/tsp/jimi_api/configurations): Configurations files.
- [controllers/](src/main/java/com/tsp/jimi_api/controllers): All the requests of the API.
- [entities/](src/main/java/com/tsp/jimi_api/entities): Entities of the database.
- [enums/](src/main/java/com/tsp/jimi_api/enums): Enums classes.
- [global/](src/main/java/com/tsp/jimi_api/global): Shared functions between multiple classes.
- [records/](src/main/java/com/tsp/jimi_api/records): Records of the input and output data.
- [repositories/](src/main/java/com/tsp/jimi_api/repositories): SQL requests to the database.

## License
This project is licensed under the MIT License. For more information, see the [LICENSE](LICENSE) file.

## Authors
- Jul SQL
