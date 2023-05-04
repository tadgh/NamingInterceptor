package com.smilecdr.demo.fhirstorage;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.api.config.DaoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class S3BlobNamingInterceptor {

	@Autowired
	private DaoConfig myDaoConfig;


	/**
	 * This removes the limits on the amount of meta tags that can be added to a given resource.
	 */
	@Hook(Pointcut.INTERCEPTOR_REGISTERED)
	public void modifyDaoConfig() {
		myDaoConfig.setResourceMetaCountHardLimit(null);
	}

}