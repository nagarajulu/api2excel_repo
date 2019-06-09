
# Api2Excel (http://api2excel.com)

Welcome to API-2-EXCEL DOCUMENT GENERATION PLATFORM. You can use this product to generate API Documentation from your API definitions 
itself and save time.

## API formats supported

The following API Articraft types are supported.
1. WSDL (SOAP WebServices)
2. JSON (Swagger / OpenAPI Specifications v2)

**Note:** Swagger / OpenAPI Specifications v3 is not yet supported. Will be planned to support in later release.

## Features of the product

API-2-EXCEL aims at saving developers quality time! It gets most of needed information in API definitions itself. Below are features:

* Generated API document will have "Request" and "Response" sheets.
* Each sheet will have the following attributes parsed from WSDL/Swagger(JSON):
  * __Element/Attribute Name:__ Name as per XSD/Swagger
  * __Data Type:__ Data Type from XSD/Swagger. For swagger, it also adds format. 
  * __Cardinality:__ 1/1 - Mandatory, 0/1 - Optional, 1/* - One-to-many, 0/*-Zero to many
  * __X-Path:__ Path from Request/Response to the specific element
  * __Description:__ Annotation/Documentation from XSD, or Description from Swagger.
* Generated API Document URLs are good for a day (i.e. next day UTC ending time); and auto expired/deleted after.
* API input files (WSDL, XSD, Swagger/JSON) are removed as soon as they are parsed, and not saved anywhere.

## Contact

If you notice an issue during API excel generation (or) want to submit feature request, please don't forget to submit an [issue](https://github.com/nagarajulu/api2excel_repo/issues/new) 

For providing any other feedback on this product, you can message me at [contact@naerakoni.me](contact@naerakoni.me)

