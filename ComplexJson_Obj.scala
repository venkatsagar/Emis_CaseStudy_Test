package Spark_Complex_Json
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.types.{ArrayType, StructType}

object ComplexJson_Obj {

  def main(args: Array[String]): Unit = {
    println("Apache Spark Application Started ...")

    val spark = SparkSession.builder()
      .appName("Create DataFrame from JSON File")
      .master("local[*]")
      .getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")

    //Code Block 1 Starts Here

    val json_file_path ="C://exa-data-eng-assessment-main//data"
    val json_data_df = spark.read.
      option("inferSchema",value = true).
      option("multiline", value = true).
      json(json_file_path)

    val fields = json_data_df.schema.fields
    val fieldnames = fields.map(x=>x.name)
    val len = fields.length

    println(fields.mkString("Array(", ", ", ")"))
    println(fieldnames)
    println(len)

    def flattenDataframe(df: DataFrame): DataFrame = {

      val fields = df.schema.fields
      val fieldNames = fields.map(x => x.name)
      val length = fields.length

      for(i <- 0 to fields.length-1){
        val field = fields(i)
        val fieldtype = field.dataType
        val fieldName = field.name
        fieldtype match {
          case arrayType: ArrayType =>
            val fieldNamesExcludingArray = fieldNames.filter(_!=fieldName)
            val fieldNamesAndExplode = fieldNamesExcludingArray ++ Array(s"explode_outer($fieldName) as $fieldName")

            // val fieldNamesToSelect = (fieldNamesExcludingArray ++ Array(s"$fieldName.*"))

            val explodedDf = df.selectExpr(fieldNamesAndExplode:_*)
            return flattenDataframe(explodedDf)
          case structType: StructType =>
            val childFieldnames = structType.fieldNames.map(childname => fieldName +"."+childname)
            val newfieldNames = fieldNames.filter(_!= fieldName) ++ childFieldnames
            val renamedcols = newfieldNames.map(x => (col(x.toString()).as(x.toString().replace(".", "_"))))
            val explodedf = df.select(renamedcols:_*)
            return flattenDataframe(explodedf)
          case _ =>
        }
      }
      df
    }

    val flattendedJSON = flattenDataframe(json_data_df)
    //schema of the JSON after Flattening
    flattendedJSON.schema

    //Output DataFrame After Flattening
    flattendedJSON.show(false)

    //Writing into CSV File

    flattendedJSON.write.format("parquet ").save("C:/emis_cs_parquet")






  }

}
