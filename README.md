# DM-Uncle Maven Plugin
Analyses your project and reports dependency information to a configured server.

# Usage
Add the code below to your pom.xml
```
<plugin>
	<groupId>io.dmuncle.maven</groupId>
	<artifactId>dmuncle-maven-plugin</artifactId>
	<configuration>
		<serverAddress>
			https://public.dmuncle.io/
		</serverAddress>
	</configuration>
	<executions>
		<execution>
			<id>gather</id>
			<phase>package</phase>
			<goals>
				<goal>dmuncle-watch</goal>
			</goals>
		</execution>
		<execution>
			<id>send</id>
			<phase>package</phase>
			<goals>
				<goal>dmuncle-send</goal>
			</goals>
		</execution>
	</executions>
</plugin>
```
From terminal run:
```
mvn dmuncle:dmuncle-watch
```
```
mvn dmuncle:dmuncle-send
```
