## Document Classification Service

A small hobbiest attempt at a document classification service. Feed the service documents and the classification of the document, do this sufficiently and the accuracy of the classification will improve.

Is still very much in development

### Usage

##### Seed<br/><br/>

    POST /seed
    {
        "title": "Web Development",
        "body": "PHP Javascript and the HTML5s"
    }

##### Classify<br/><br/>

    POST /classify
    Javascript

##### Outcome<br/><br/>

    1: Web Development

