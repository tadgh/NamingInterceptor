This is a sample HAPI-FHIR Storage interceptor, which prefixes bulk export binaries by their extension metadata fields, such as export ID, and resource type.


To create the jar: 

```
mvn clean install -DskipTests
```

Find the resulting `.jar` file in the target folder. Copy it to Smile CDR's `customerlib/` directory. 
Under the persistence storage setting `Interceptor Bean Types`, type `com.smilecdr.demo.fhirstorage.S3BlobNamingInterceptor`, and then restart the persistence module. 


