package com.smilecdr.demo.fhirstorage;

import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.interceptor.model.ReadPartitionIdRequestDetails;
import ca.uhn.fhir.interceptor.model.RequestPartitionId;
import ca.uhn.fhir.interceptor.model.TransactionWriteOperationsDetails;
import ca.uhn.fhir.jpa.api.model.DeleteConflictList;
import ca.uhn.fhir.jpa.api.model.ResourceVersionConflictResolutionStrategy;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.api.InterceptorInvocationTimingEnum;
import ca.uhn.fhir.rest.api.server.IPreResourceAccessDetails;
import ca.uhn.fhir.rest.api.server.IPreResourceShowDetails;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.bulk.BulkDataExportOptions;
import ca.uhn.fhir.rest.api.server.storage.DeferredInterceptorBroadcasts;
import ca.uhn.fhir.rest.api.server.storage.TransactionDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import ca.uhn.fhir.util.StopWatch;
import ca.uhn.fhir.validation.ValidationResult;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class S3BlobNamingInterceptor {

	private static final Logger ourLog = LoggerFactory.getLogger(S3BlobNamingInterceptor.class);

	@Hook(Pointcut.STORAGE_BINARY_ASSIGN_BLOB_ID_PREFIX)
	public String storageAssignBlobIdPrefix(RequestDetails theRequestDetails, IBaseResource theBinaryResource) {
		ourLog.info("Interceptor STORAGE_BINARY_ASSIGN_BLOB_ID_PREFIX - started");
		StopWatch stopWatch = new StopWatch();
		try {
			ourLog.info("Received Binary for prefixing!") ;
			IBaseHasExtensions meta = (IBaseHasExtensions) theBinaryResource.getMeta();
			ourLog.info("Here are the available extensions on the resource metadata.");
			meta.getExtension().forEach(ext -> ourLog.info("Extension: " + ext.getUrl() + " " + ext.getValue().toString()));

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
		} finally {
			ourLog.info("Interceptor STORAGE_BINARY_ASSIGN_BLOB_ID_PREFIX  - ended, execution took {}", stopWatch);
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