# Sherpa
Sherpa is a serialization toolkit and a 'reflection-less' case classes mapper for the [Scala](http://www.scala-lang.org) programming language.

 * sherpa-core: Serialization toolkit with compile time (macro-oriented) case classes mapping
 * sherpa-jackson: [Jackson](http://wiki.fasterxml.com/JacksonHome) binding
 * sherpa-scalaz: [Scalaz](http://github.com/scalaz/scalaz) (7-M1) integration module

As this project is using macro, it need Scala 2.10

**Sherpa is currently in early phase**

## Example usage

    import sherpa._
    import sherpa.serializer.jackson._

    case class Person(name: String, age: Int, emails: Seq[String]) // TODO Fix why it fail with List

    val mapper = Mapper[Person]
    import mapper._

    val person = Person("Alois Cochard", 27, List("alois.cochard@gmail.com", "alois.cochard@opencredo.com"))

    // Using mapper directly (serializer resolved implicitly)
    parse(generate(alois)) // = Right(Person(Alois Cochard,27,Stream(alois.cochard@gmail.com, ?)))

    // Using serializer directly (mapper resolved implicitly)
    Jackson.parse[Person](Jackson.generate(alois)) // = Right(Person(Alois Cochard,27,Stream(alois.cochard@gmail.com, ?)))
      

    // Or using the Scalaz integration (sherpa-scalaz needed)
    import sherpa.scalaz._
    
    serializer(Jackson).generate(alois)
    // = IO[ByteString]
      .map(bytes => serializer(Jackson).parse[Person](bytes))
    // = IO[Validation[SerializerError, Person]]
      .unsafePerformIO()
    // = Success(Person(Alois Cochard,27,Stream(alois.cochard@gmail.com, ?))))
    
## Contribution Policy

Contributions via GitHub pull requests are gladly accepted from their original author.
Along with any pull requests, please state that the contribution is your original work and 
that you license the work to the project under the project's open source license.
Whether or not you state this explicitly, by submitting any copyrighted material via pull request, 
email, or other means you agree to license the material under the project's open source license and 
warrant that you have the legal authority to do so.

## License

    This software is licensed under the Apache 2 license, quoted below.

    Copyright 2012 Alois Cochard 

    Licensed under the Apache License, Version 2.0 (the "License"); you may not
    use this file except in compliance with the License. You may obtain a copy of
    the License at http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
    License for the specific language governing permissions and limitations under
    the License.
