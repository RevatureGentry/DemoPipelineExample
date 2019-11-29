# DemoPipelineExample
## Getting the application to deploy
* As we all knew Wednesday afternoon, there was a terribly frustrating `java.util.zip.ZipException`
  * I found out that in my case, this was being caused from the ojdbc7.jar file being corrupt
  * Please follow the same steps here if you also had this issue

## 0 - Setup from the very beginning
* We completed all of these steps during the day Wednesday, but just to make sure we are all caught up

## 1.0 - EC2
* Grab an Ubuntu 18.04 t2.micro EC2 instance

## 2.0 - Dependencies
* Once interacting with the EC2 instance through SSH
  * `sudo apt update`
  * `sudo apt upgrade -y`
  * `sudo apt install openjdk-8-jdk-headless`
  * `sudo apt install maven`
  * `sudo apt install tomcat9 tomcat9-admin`

* Install Jenkins by [following `Step 1` diretions here](https://www.digitalocean.com/community/tutorials/how-to-install-jenkins-on-ubuntu-18-04)

## 3.0 - Environment Variables
* With elevated privileges, add the following to `/etc/environment` below the leading `PATH` variable
  ```bash
  export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
  export JRE_HOME=/usr/lib/jvm/java-8-openjdk-amd64/jre
  export M2_HOME=/usr/share/maven
  export M2=$M2_HOME/bin
  export PATH=$M2:$PATH
  ```

* Execute `source /etc/environment` to see those changes reflected
* Execute the following to verify that the environment variables are set properly
  ```bash
  ubuntu@ec2-host:~$ $JAVA_HOME/bin/javac -version
  ubuntu@ec2-host:~$ $JRE_HOME/bin/java -version
  ubuntu@ec2-host:~$ $M2_HOME/bin/mvn -version
  ```

## 4.0 - Tomcat Configuration
* This is largely the same as how we did it together

## 4.1 - Tomcat User Configuration
* This allows us to deploy applications
* Modify the `/etc/tomcat9/tomcat-users.xml` and provide the following just above the closing `<tomcat-users>` tag
  ```xml
  <role rolename="manager-gui" />
  <role rolename="manager-script" />
  <user username="tomcat" password="password" roles="manager-gui,manager-script" />
  ```

## 4.2 - Tomcat Connection Configuration
* This allows us to connect to Tomcat from anywhere
* In **_BOTH_** of the following (`/usr/share/tomcat9-admin/manager/META-INF/context.xml` and `/usr/share/tomcat9-admin/host-manager/META-INF/context.xml`), comment out the `<Valve>` element as follows
  ```xml
  <Context antiResourceLocking="false" privileged="true" >
  <!--  <Valve className="org.apache.catalina.valves.RemoteAddrValve"
          allow="127\.\d+\.\d+\.\d+|::1|0:0:0:0:0:0:0:1" /> -->
    <Manager sessionAttributeValueClassNameFilter="java\.lang\.(?:Boolean|Integer|Long|Number|String)|org\.apache\.catalina\.filters\.CsrfPreventionFilter\$LruCache(?:\$1)?|java\.util\.(?:Linked)?HashMap"/>
  </Context>
  ```

## 4.3 - Tomcat Request Size
* The following allows Tomcat to accept rather large .war files
  * In the `/usr/share/tomcat9-admin/manager/WEB-INF/web.xml` file, change the value to 134217728 for the `<max-file-size>` and `<max-request-size>` tags under the first `<multipart-config>` tag you come across
  ```xml
    <multipart-config>
      <!-- 50MB max -->
      <max-file-size>134217728</max-file-size>
      <max-request-size>134217728</max-request-size>
      <file-size-threshold>0</file-size-threshold>
    </multipart-config>
  ```


## 4.4 - Tomcat Port Configuration
* Because Jenkins is running on port 8080
  * Modify the `/etc/tomcat9/server.xml` file, and at the first `<Connector>` element you come across, change the port value to be 9000
  ```xml
      <Connector port="9000" protocol="HTTP/1.1"
               connectionTimeout="20000"
               redirectPort="8443" />
  ```

## 5.0 - Jenkins Configuration
* All of this information can also be found in the document I circulated Wednesday Afternoon

## 5.1 - Download Suggested Plugins
* Also grab the `Deploy to container` plugin, as we will need this later

## 5.2 - Manage Jenkins -> Configure System
* Add the Github Server
  * Configure a Personal Access Token and make sure you check `Manage Hooks` in Jenkins
* Save

## 5.3 - Manage Jenkins -> Global Tool Configuration
* Maven Configuration (top of page)
  * Specify the filepath in both choices to be `/etc/maven/settings.xml`
* JDK Installation
  * Provide `Java 8` for the name, and specify JAVA_HOME as `/usr/lib/jvm/java-8-openjdk-amd64/` 
* Maven Installation
  * Provide `Maven` for the name, and specify MAVEN_HOME as `/usr/share/maven`
* Save

## 5.5 - Configure Jenkins Job
* From the dashboard, select `New Item` in the top left corner
  * Select `Freestyle Project` and give it a name

## 5.5.1 - `General` Tab
* Populate the GitHub Project URL

## 5.5.2 - `Source Code Management` Tab
* Select the Git radio button
  * Specify the clone URL 
  * Create (if not already created) Credentials of Kind `Username with Password`
    * **_YOUR_** Github Username is the username
    * **_YOUR_** Github Password is the password
  * Additionally, specify which branches this job should run

## 5.5.3 - `Build Triggers` Tab
* Check the value `GitHub hook trigger for GITScm polling`

## 5.5.4 - `Build Environment` Tab (**_OPTIONAL_**)
* Select the `Abort the build if it's stuck` option
  * I specified 5 minutes timeout 

## 5.5.5 - `Build` Tab
* Select the `Invoke top-level Maven targets` option
  * Choose our maven version, not the `(Default)`
  * Specify `-e -U clean package`

## 5.5.6 - `Post-build Actions` Tab
* Select the `Deploy to container` option
  * Specify `**/*.war` for the WAR/EAR files
  * Specify the name of your project (for example, mine was `/PipelineDemoApplication`) as the Context path
  * Containers
    * Select `Tomcat 9.x Remote`
    * For the Credentials, create (if not already created) Credentials of kind `Username with Password`
      * Username = whatever value you put for the username in `/etc/tomcat9/tomcat-users.xml`
      * Password = whatever value you put for the password in `/etc/tomcat9/tomcat-users.xml`
    * Tomcat URL should be `http://localhost:9000`

# Now, for the most important part
## 6.0 - Fixing what went wrong on Wednesday

## 6.0.1 - Determine if JDBC Jar file on EC2 is corrupt
* On your EC2, locate where your `ojdbc7.jar` file is located
  * If you **_do not_** see a `java.util.zip.ZipException` when you run the command `jar tf ojdbc7.jar` **ON YOUR EC2**
    * Run this command: `sudo cp ojdbc7.jar /var/lib/tomcat9/lib`
    * Verify using this command `sudo jar tf /var/lib/tomcat9/lib/ojdbc7.jar`
  * If you **_DO_** see a `java.util.zip.ZipException` on your EC2, then from **YOUR MACHINE**
    * You need to `pscp` or `scp` the jar file from your machine to the EC2
      * `pscp -i <absolute>/<path>/<to>/<your>/private-key.ppk <absolute>/<path>/<to>/<your>/ojdbc7.jar ubuntu@<your-ec2-dns-name>:/tmp`
      * Please resolve everything in between <> with your values
      * Note that the jar file can be found at `/tmp/ojdbc7.jar` on your EC2 instance
  * Once the `ojdbc7.jar` is on your EC2 instance
    * Verify using this command `sudo jar tf /tmp/ojdbc7.jar`
    * Run this command: `sudo cp /tmp/ojdbc7.jar /var/lib/tomcat9/lib`
    * Verify using this command `sudo jar tf /var/lib/tomcat9/lib/ojdbc7.jar`

## 6.0.2 - Fix the `pom.xml` in **_YOUR_** project
* In **_YOUR_** project modify the jdbc dependency as follows
```xml
<dependency>
  <groupId>com.oracle</groupId>
  <artifactId>ojdbc7</artifactId>
  <version>12.1.0</version>
  <scope>provided</scope> <!-- This is crucial; allows the project to depend on Tomcat for this -->
</dependency>
```

## 6.0.3 - Run a Build
* At this point, your pipeline _should_ be configured properly. Please run a new build and reach out to me on Slack with any issues.