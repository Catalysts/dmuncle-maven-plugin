DM-Uncle Maven Plugin

Setup: add the code below to your pom.xml
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

Run: first run the dmuncle-watch goal in the maven task panel (or in command line type: mvn dmuncle:dmuncle-watch)
second run the dmuncle-send goal in the maven task panel (or in command line type: mvn dmuncle:dmuncle-send)
