package com.herfy.utils.helper;

import com.herfy.model.ServerType;
import com.herfy.utils.BuildConfig;

public class EnvUtil {

	private static final String PRODUCTION_BASE_URL = "https://api.solo.skylinedynamics.com/";
	private static ServerType serverType = null;

	public static void setCustomEnvironment(ServerType _serverType){
		serverType = _serverType;
	}

	public static String getEnvironmentUrl(){
		if (serverType != null && !serverType.getUrl().isEmpty()) {
			return serverType.getUrl();
		} else {
			return BuildConfig.BASE_URL;
		}
	}

	public static boolean isProduction() {
		if(serverType != null) {
			return serverType.isProd();
		} else {
			return getEnvironmentUrl().equalsIgnoreCase(PRODUCTION_BASE_URL);
		}
	}

	public static String getContentUrlPrefix() {
		if(serverType != null) {
			return serverType.getContentUrlPrefix();
		}
		return "production/";
	}
}
