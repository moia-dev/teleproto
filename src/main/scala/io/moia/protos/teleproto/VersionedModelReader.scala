/*
 * Copyright 2019 MOIA GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.moia.protos.teleproto

import java.io.InputStream

import com.google.protobuf.CodedInputStream
import scalapb.json4s.Parser
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, Message}

import scala.collection.immutable.ListMap
import scala.util.{Failure, Success, Try}

/**
  * The versioned model reader is a lookup table for the specific version models (ScalaPB classes) for some particular
  * detached model.
  * The lookup table is a mapping from a (generic) version to a the model in that version.
  * Those are combined with readers from that version models to the expected detached model.
  *
  * The interface allows to parse bytes/json directly into the detached model type for a given version that is in the
  * lookup table.
  *
  * The generic version type should be independent of the parsing as JSON or bytes.
  * That is, a Mime Type is not a good version type.
  *
  * Usage Example:
  *
  * {{{
  * implicit val someModelReader: ModelReader[SomeDetachedModel] =
  *    new ModelReader[MyVersion, SomeDetachedModel] {
  *      val readerMappings: ReaderMappings =
  *        ListMap(
  *          VN -> readerMapping(vN.SomeApiModel),
  *          ...
  *          V2 -> readerMapping(v2.SomeApiModel),
  *          V1 -> readerMapping(v1.SomeApiModel)
  *        )
  *    }
  * }}}
  */
trait VersionedModelReader[Version, DetachedModel] {

  /**
    * Parse JSON as the versioned model in given version and read the detached model from the result.
    *
    * @return the parsed model
    */
  def fromJson(jsonString: String, version: Version): Try[PbResult[DetachedModel]] =
    lookupOrFail(version).flatMap(_.fromJson(jsonString))

  /**
    * Parse proto in specific version and read the detached model from the result.
    *
    * @return the parsed model
    */
  def fromProto(input: CodedInputStream, version: Version): Try[PbResult[DetachedModel]] =
    lookupOrFail(version).flatMap(_.fromProto(input))

  def fromProto(input: Array[Byte], version: Version): Try[PbResult[DetachedModel]] =
    fromProto(CodedInputStream.newInstance(input), version)

  def fromProto(input: InputStream, version: Version): Try[PbResult[DetachedModel]] =
    fromProto(CodedInputStream.newInstance(input), version)

  /**
    * Exposes the supported versions.
    *
    * @return the supported versions
    */
  def supportedReaderVersions: Set[Version] = readerMappings.keySet

  /**
    * Looks up the version reader for a specific version.
    *
    * Implementation can override the matching strategy for looked up and provided versions.
    *
    * E.g. a matching check could be performed just on a major version if the `Version` type contains major and minor.
    *
    * @param version the version that should be read
    * @return the matching reader or none
    */
  def lookupReader(version: Version): Option[VersionReader] =
    readerMappings.get(version)

  protected type ReaderMappings = ListMap[Version, VersionReader]

  /**
    * Maps the supported versions to a reader in the particular version.
    */
  def readerMappings: ReaderMappings

  /**
    * For a companion of a specific ScalaPB class looks up the corresponding reader to the detached model.
    * If that is available constructs are reader directly from bytes/json to the detached model.
    */
  protected def readerMapping[SpecificModel <: GeneratedMessage with Message[SpecificModel]](
      companion: GeneratedMessageCompanion[SpecificModel]
  )(implicit reader: Reader[SpecificModel, DetachedModel]): VersionReader =
    new VersionReaderImpl(companion, reader)

  /**
    * Models a reader from bytes/json directly to the detached business model.
    */
  protected trait VersionReader {
    def fromJson(jsonString: String): Try[PbResult[DetachedModel]]
    def fromProto(input: CodedInputStream): Try[PbResult[DetachedModel]]
  }

  private def lookupOrFail(version: Version): Try[VersionReader] =
    lookupReader(version).map(Success(_)).getOrElse(Failure(new VersionNotSupportedException(version, supportedReaderVersions)))

  /**
    * Combines a generated message companion (able to read bytes/json in to a specific ScalaPB model)
    * and a reader from that ScalaPB model to a detached model.
    * That models a reader from bytes/json directly to the detached business model.
    */
  private class VersionReaderImpl[SpecificModel <: GeneratedMessage with Message[SpecificModel]](
      companion: GeneratedMessageCompanion[SpecificModel],
      reader: Reader[SpecificModel, DetachedModel]
  ) extends VersionReader {
    def fromJson(jsonString: String): Try[PbResult[DetachedModel]] =
      Try(VersionedModelReader.parser.fromJsonString(jsonString)(companion)).map(reader.read)

    def fromProto(input: CodedInputStream): Try[PbResult[DetachedModel]] =
      Try(companion.parseFrom(input)).map(reader.read)
  }
}

object VersionedModelReader {

  private val parser = new Parser()
}
