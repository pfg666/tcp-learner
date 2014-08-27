package sut.interfacing;

import java.util.List;

import sut.info.SutInfo;
import sut.interfacing.init.ActiveInitChecker;
import sut.interfacing.init.CacheBuilder;
import sut.interfacing.init.CacheManager;
import sut.interfacing.init.CachedInitOracle;
import sut.interfacing.init.InitCache;
import sut.interfacing.init.InitChecker;
import sut.interfacing.init.InitOracle;
import sut.interfacing.init.StoringInitCache;
import sut.mapper.TCPMapper;
import abslearning.learner.TCPConfig;

/**
 * Factory which builds the main components needed to learn TCP from a TCP configuration object. 
 * That is: the <b>TCP mapper</b> which uses an <b> initialization oracle </b> to update its state variables
 * after every input. 
 */
public class TCPBuilder {
	
	public static TCPMapper buildMapper(TCPConfig config) {
		TCPMapper builtMapper = null;
		if(config.oracle.toLowerCase().equals("adaptive")) {
			InitOracle builtOracle = buildAdaptiveOracle(config);
			builtMapper = new TCPMapper(builtOracle);
		} else {
			try {
				builtMapper = (TCPMapper) Class.forName("sut.mapper.tested."+config.oracle.toUpperCase()+"Mapper").newInstance();
			} catch (InstantiationException | IllegalAccessException
					| ClassNotFoundException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
		return builtMapper;
	}
	
	public static InitOracle buildOracle(TCPConfig config) {
		InitOracle builtOracle = null;
		if(config.oracle.toLowerCase().equals("adaptive")) {
			builtOracle = buildAdaptiveOracle(config);
		} else {
			try {
				builtOracle = (InitOracle) Class.forName("sut.mapper.tested."+config.oracle.toUpperCase()+"Mapper").newInstance();
			} catch (InstantiationException | IllegalAccessException
					| ClassNotFoundException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
		return builtOracle;
	}
	
	/**
	 * An active oracle is an oracle which infers responses actively, by querying the system.
	 */
	private static InitOracle buildAdaptiveOracle(TCPConfig config) {
		InitChecker initChecker = new ActiveInitChecker(config.learningPort);
		InitCache initCache = new StoringInitCache();
		System.out.println(config.maximumTraceNumber);
		if(config.prebuildCache == true) {
			List<String> inputs = SutInfo.getInputStrings();
			CacheBuilder builder = new CacheBuilder(initChecker, initCache);
			builder.buildInitCache(inputs.toArray(new String[inputs.size()]), config.maximumTraceNumber);
			CacheManager cm = new CacheManager();
			cm.dump(config.CACHE_FILE);
		}
		CachedInitOracle builtOracle = new CachedInitOracle(initChecker, initCache);
		return builtOracle;
	} 
}
