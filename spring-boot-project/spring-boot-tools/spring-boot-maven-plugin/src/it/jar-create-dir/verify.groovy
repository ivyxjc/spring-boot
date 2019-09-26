import org.springframework.boot.maven.Verify

Verify.verifyJar(
		new File(basedir, "target/foo/jar-create-dir-0.0.1.BUILD-SNAPSHOT-foo.jar"), "some.random.Main"
)
