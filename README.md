dataTXT-NEX Stanbol Enhancement Engine (datatxtNex)
===================================================

_datatxtNex_ is an
[Enhancement Engine](https://stanbol.apache.org/docs/trunk/components/)
for [Apache Stanbol](https://stanbol.apache.org/) which allows you to
use [dataTXT](https://dandelion.eu/products/datatxt/) to annotate your
text from Apache Stanbol.

It supports both Stanbol FISE and the Fusepool Annotation Model (FAM)
as output ontologies, and can be easily integrated into the
[Fusepool P3](http://fusepoolp3.github.io/) platform through the
[P3 Stanbol Enhancer Adapter](https://github.com/fusepoolP3/stanbol-enhancer-adapter).

_datatxtNex_ calls the dataTXT APIs to annotate your text. To use the
APIs, you need to
[register first](https://dandelion.eu/accounts/login/) and obtain a
**application key** and an **application id**. It's free.
 
Configuration Parameters
========================

The engine supports a number of configuration parameters, the most
relevant of which are:

* **Output ontology** `eu.spaziodati.datatxt.stanbol.enhancer.engines.outputOntology`:
  allows the configuration of the ontology in which to produce the
  entity annotations. The currently supported values are:

  * `FISE`: produces annotations in the [Stanbol FISE]() ontology;
  * `FAM`: produces annotations in the [Fusepool Annotation Model]().

* **Application ID**
  (`eu.spaziodati.datatxt.stanbol.enhancer.engines.app_id`) and
  **application key**
  (`eu.spaziodati.datatxt.stanbol.enhancer.engines.app_key`): should
  contain your dataTXT application key and id.

* **Confidence threshold**: a value between `0` and `1` which
  specifies the confidence threshold above which annotations are
  considered to be valid. Set a high threshold to obtain higher
  precision (but fewer annotations), or a low threshold to obtain more
  annotations, but with lower precision.

