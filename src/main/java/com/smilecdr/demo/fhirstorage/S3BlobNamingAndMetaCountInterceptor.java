package com.smilecdr.demo.fhirstorage;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

public class S3BlobNamingAndMetaCountInterceptor {

	@Autowired
	private DaoConfig myDaoConfig;

	private static final Logger ourLog = LoggerFactory.getLogger(S3BlobNamingAndMetaCountInterceptor.class);

	/**
	 * This removes the limits on the amount of meta tags that can be added to a given resource.
	 */
	@Hook(Pointcut.INTERCEPTOR_REGISTERED)
	public void modifyDaoConfig() {
		myDaoConfig.setResourceMetaCountHardLimit(null);
	}
	@Hook(Pointcut.STORAGE_BINARY_ASSIGN_BLOB_ID_PREFIX)
	public String storageAssignBlobIdPrefix(RequestDetails theRequestDetails, IBaseResource theBinaryResource) {
			IBaseHasExtensions meta = (IBaseHasExtensions) theBinaryResource.getMeta();
			// Export ID
			Optional<String> exportId = getExtensionByUrl(meta, JpaConstants.BULK_META_EXTENSION_EXPORT_IDENTIFIER);
			// Resource Type
			Optional<String> resourceType = getExtensionByUrl(meta, JpaConstants.BULK_META_EXTENSION_RESOURCE_TYPE);
			// Job ID
			Optional<String> jobId = getExtensionByUrl(meta, JpaConstants.BULK_META_EXTENSION_JOB_ID);
			if (exportId.isPresent() && resourceType.isPresent()) {
				return buildExportIdAndResourceTypePrefix(exportId.get(), resourceType.get());
			} else if (jobId.isPresent()) {
				return buildMissingExportIdPrefix(jobId.get());
			} else {
				ourLog.error("Unable to determine prefix for binary resource: {}", theBinaryResource.getIdElement().toUnqualifiedVersionless().getValue());
				return "";
			}
	}

	/**
	 * Builds a prefix like "my-export-identifier/Patient/"
	 */
	private String buildExportIdAndResourceTypePrefix(String theExportId, String theResourceType) {
		return theExportId + "/" + theResourceType + "/";
	}

	/**
	 * Builds a prefix like "missing-export-identifier/uuid-1234-abcd-efgh/"
	 */
	private String buildMissingExportIdPrefix(String theJobId) {
		return "missing-export-identifier/" + theJobId + "/";
	}

	/**
	 * Given a URL for an extension, return an optional containing the string value of that extension, if present. Otherwise, return an empty optional.
	 */
	private Optional<String> getExtensionByUrl(IBaseHasExtensions theExtendable, String theExtensionUrl) {
		return theExtendable.getExtension().stream().filter(ext -> ext.getUrl().equalsIgnoreCase(theExtensionUrl)).map(ext -> ext.getValue().toString()).findFirst();
	}
}