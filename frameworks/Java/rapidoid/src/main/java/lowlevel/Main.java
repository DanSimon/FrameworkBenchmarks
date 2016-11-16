package lowlevel;

import org.rapidoid.config.Conf;

public class Main {

	public static void main(String[] args) throws Exception {
		Conf.HTTP.set("maxPipeline", 16);
		new PlaintextAndJsonServer().listen(8080);
	}

}
