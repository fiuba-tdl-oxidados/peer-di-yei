package uba.fi.verysealed.rocola.behavior.playlist
import play.api.libs.json._
import uba.fi.verysealed.rocola.behavior.SongMetadata

import scala.io.Source
import scala.util.Using

object SongMetadataImporter {


  // Implicit reads for SongMetadata
  implicit val songMetadataReads: Reads[SongMetadata] = Json.reads[SongMetadata]


  def importSongsFromJson(filePath: String): List[SongMetadata] = {
    try {
      val resource = getClass.getClassLoader.getResource(filePath)
      if (resource == null) {
        println(s"Resource not found: $filePath")
        List.empty[SongMetadata]
      } else {
        val source = Source.fromURL(resource)
        try {
          val jsonString = source.getLines().mkString
          println(s"JSON content: $jsonString") // Debugging statement
          val json = Json.parse(jsonString)
          json.validate[List[SongMetadata]] match {
            case JsSuccess(songs, _) =>
              println(s"Successfully parsed songs: $songs") // Debugging statement
              songs
            case JsError(errors) =>
              println(s"Error parsing JSON: $errors")
              List.empty[SongMetadata]
          }
        } catch {
          case e: Exception =>
            println(s"Exception occurred while reading the file: ${e.getMessage}")
            List.empty[SongMetadata]
        } finally {
          source.close()
        }
      }
    } catch {
      case e: Exception =>
        println(s"Exception occurred while accessing the file: ${e.getMessage}")
        List.empty[SongMetadata]
    }
  }



}
