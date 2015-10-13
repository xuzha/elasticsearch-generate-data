# elasticsearch-generate-data
Generate and upload test data to  Elasticsearch, in Scala

## Run with sbt
Exampe : sbt "run -count=5000 -es_url=http://localhost:9200/ -index_name=test"


Support the following configurations
  Options:
    -h, --help
       Print help text
       Default: false
    -batch_size
       Elasticsearch bulk index batch size
       Default: 500
    -count
       Number of docs to generate
       Default: 10000
    -delete_index
       Set delete index first
       Default: false
    -es_url
       URL of your Elasticsearch node
       Default: http://localhost:9200/
    -format
       message format
       Default: name:String,age:Int,last_updated:ts
    -index_name
       Name of the index to store your messages
       Default: test_data
    -index_type
       Type
       Default: test_type
    -num_of_replicas
       Number of replicas for ES index
       Default: 0
    -num_of_shards
       Number of shards for ES index
       Default: 2
    -set_refresh
       Set refresh rate to -1 before starting the upload
       Default: false
    -user_name
       For Shield
	-password
       For Shield