import org.springframework.boot.maven.Verify

Verify.verifyWar(
		new File(basedir, "target/war-0.0.1.BUILD-SNAPSHOT.war")
)

