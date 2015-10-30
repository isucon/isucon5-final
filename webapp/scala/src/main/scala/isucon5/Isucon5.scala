package isucon5

import java.io.BufferedInputStream
import java.net.{HttpURLConnection, URI, URLEncoder}
import java.nio.charset.CodingErrorAction
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.sql.{Connection, DriverManager, PreparedStatement, ResultSet, SQLException}
import java.time.format.DateTimeFormatter
import java.util.{Calendar, TimeZone}
import javax.net.ssl._

import org.postgresql.ds.PGPoolingDataSource
import org.slf4j.LoggerFactory
import skinny.micro.WebApp
import skinny.micro.contrib.ScalateSupport
import xerial.core.util.Shell

import scala.io.{Codec, Source}
import scala.util.Random


sealed trait Grade
object Grade {
  case object micro extends Grade
  case object small extends Grade
  case object standard extends Grade
  case object premium extends Grade

  private val table = Seq(micro, small, standard, premium).map(v => v.toString -> v).toMap
  def fromName(name: String): Grade = table(name.toLowerCase)
}

case class User(id: Int, email: String, grade: Grade) {
  def this(rs: ResultSet) = this(rs.getInt("id"), rs.getString("email"), Grade.fromName(rs.getString("grade")))
}
case class Endpoint(meth: String, tokenType: String, tokenKey: String, uri: String) {
  def this(rs: ResultSet) = this(rs.getString("meth"), rs.getString("token_type"), rs.getString("token_key"), rs.getString("uri"))
}
case class Subscription(userId: Int, arg: String)

/**
 *
 */
object Isucon5 extends WebApp with ScalateSupport {

  case object AuthenticationError extends Exception
  case object PermissionDenied extends Exception
  case object ContentNotFound extends Exception

  object DB {
    private val logger = LoggerFactory.getLogger("isucon5.DB")
    val cal = Calendar.getInstance(TimeZone.getDefault)
    val df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private implicit class RichResultSet(rs: ResultSet) {
      def getLocalDateTime(colName: String) = rs.getTimestamp(colName, cal).toLocalDateTime
    }

    // database configuration
    case class DBConfig(host: String,
                        port: Int,
                        user: String,
                        password: Option[String],
                        name: String,
                        jdbcDriverName: String,
                        jdbcProperties: Map[String, String]
                         ) {
      lazy val jdbcUrl = {
        var props = jdbcProperties + ("user" -> user)
        password.map(p => props += "password" -> p)
        s"jdbc:postgresql://${host}:${port}/${name}?${props.map { case (k, v) => s"${k}=${v}" }.mkString("&")}"
      }
    }

    private def env = System.getenv()

    lazy val dbConfig: DBConfig = DBConfig(
      host = env.getOrDefault("ISUCON5_DB_HOST", "localhost"),
      port = env.getOrDefault("ISUCON5_DB_PORT", "5432").toInt,
      user = env.getOrDefault("ISUCON5_DB_USER", "isucon"),
      password = Option(System.getenv.get("ISUCON5_DB_PASSWORD")),
      name = env.getOrDefault("ISUCON5_DB_HOST", "isucon5f"),
      jdbcDriverName = "org.postgresql.Driver",
      jdbcProperties = Map("connectTimeout" -> "3600", "stringtype" -> "unspecified", "charSet" -> "UTF-8")
    )

    // Query execution helper methods
    def withResource[Resource <: AutoCloseable, U](resource: Resource)(body: Resource => U): U = {
      try {
        body(resource)
      }
      finally {
        resource.close()
      }
    }

    lazy val dbSource = {
      val s = new PGPoolingDataSource()
      s.setDataSourceName("pgsource")
      s.setServerName(dbConfig.host)
      s.setPortNumber(dbConfig.port)
      s.setDatabaseName(dbConfig.name)
      s.setUser(dbConfig.user)
      dbConfig.password.map(s.setPassword(_))
      for ((k, v) <- dbConfig.jdbcProperties) {
        s.setProperty(k, v)
      }
      s
    }

    private def withConnection[A](handler: Connection => A): A = {
      withResource(dbSource.getConnection) { conn =>
        handler(conn)
      }
    }

    def executeQuery[A](sql: String, args: Any*)(resultMapper: ResultSet => A): Seq[A] = {
      withConnection { conn => conn.executeAndGet(sql, args: _*)(resultMapper) }
    }

    def execute(sql: String, args: Any*): Unit = {
      withConnection { conn => conn.execute(sql, args: _*) }
    }

    implicit class RichConnection(conn: Connection) {
      def execute[A](sql: String, args: Any*) {
        executePrep(sql, args: _*)(_.execute())
      }

      def executePrep[A](sql: String, args: Any*)(handler: PreparedStatement => A): A = {
        withResource(conn.prepareStatement(sql)) { st =>
          // populate the placeholders in the prepared statement
          for ((a, i) <- args.zipWithIndex) {
            st.setObject(i + 1, a)
          }
          handler(st)
        }
      }

      def executeAndGet[A](sql: String, args: Any*)(resultMapper: ResultSet => A): Seq[A] = {
        executePrep(sql, args: _*) { st =>
          val hasResult = st.execute()
          val b = Seq.newBuilder[A]
          if (hasResult) {
            val rs = st.getResultSet
            while (rs.next()) {
              b += resultMapper(rs)
            }
          }
          b.result()
        }
      }
    }

    def transaction[U](body: Connection => U): U = {
      Class.forName(dbConfig.jdbcDriverName)
      withResource(DriverManager.getConnection(dbConfig.jdbcUrl)) { conn =>
        try {
          conn.setAutoCommit(false)
          val ret = body(conn)
          conn.commit()
          ret
        }
        catch {
          case e: SQLException =>
            conn.rollback()
            throw e
        }
        finally {
          conn.setAutoCommit(true)
        }
      }
    }

  }

  import DB._

  private def authenticate(email: String, password: String) {
    executeQuery(
      "SELECT id, email, grade FROM users WHERE email=? AND passhash=digest(salt || ?, 'sha512')",
      email,
      password)(new User(_)).headOption match {
      case Some(user) =>
        session.setAttribute("user_id", user.id)
      case None =>
        throw AuthenticationError
    }
  }

  private def ensureLogin[U](onSuccess: User => U): U = {
    session.get("user_id") match {
      case Some(userId) =>
        executeQuery("SELECT id,email,grade FROM users WHERE id=?", userId)(new User(_))
        .headOption match {
          case Some(u) =>
            onSuccess(u)
          case None =>
            throw PermissionDenied
        }
      case None =>
        throw PermissionDenied
    }
  }


  before() {
    contentType = "text/html"
  }

  error {
    case AuthenticationError =>
      status = 401
      ssp("/login.ssp")
    case PermissionDenied =>
      status = 302
      redirect("/login")
  }


  get("/signup") {
    session.clear()
    ssp("/signup.ssp")
  }

  post("/signup") {
    val email = params("email")
    val password = params("password")
    val grade = params("grade")
    val salt = generateSalt
    val insertUserQuery =
      """INSERT INTO users (email,salt,passhash,grade)
        |VALUES (?,?,digest(? || ?, 'sha512'),?) RETURNING id
      """.stripMargin
    val insertSubscriptionQuery =
      """
        |INSERT INTO subscriptions (user_id,arg) VALUES (?,?)
      """.stripMargin
    transaction { conn =>
      val userId = conn.executeAndGet(insertUserQuery, email, salt, salt, password, grade) { rs: ResultSet =>
        rs.getInt(1)
      }.head
      conn.execute(insertSubscriptionQuery, userId, "{}")
    }
    redirect("/login")
  }

  post("/cancel") {
    redirect303("/signup")
  }

  get("/login") {
    session.clear()
    ssp("/login.ssp")
  }

  post("/login") {
    authenticate(params("email"), params("password"))
    redirect303("/")
  }

  get("/logout") {
    session.clear()
    redirect("/login")
  }

  get("/") {
    ensureLogin { u =>
      ssp("/main.ssp", "user" -> u)
    }
  }

  get("/user.js") {
    ensureLogin { u =>
      contentType = "application/javascript"
      ssp("/userjs.ssp", "grade" -> u.grade)
    }
  }

  get("/modify") {
    ensureLogin { u =>
      val arg = executeQuery("SELECT arg FROM subscriptions WHERE user_id=?", u.id)(rs => rs.getString("arg")).headOption
      ssp("/modify.ssp", "user" -> u, "arg" -> arg.getOrElse(null))
    }
  }

  import com.fasterxml.jackson.databind.ObjectMapper
  import com.fasterxml.jackson.module.scala.DefaultScalaModule
  import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

  // JSON parser
  private val mapper = {
    val m = new ObjectMapper() with ScalaObjectMapper
    m.registerModule(DefaultScalaModule)
    m
  }

  private def merge(a: Any, b: Any): Any = {
    (a, b) match {
      case (m1: Map[String, Any], m2: Map[String, Any]) =>
        val m = for (k: String <- (m1.keySet ++ m2.keySet)) yield k -> merge(m1.get(k), m2.get(k))
        m.toMap[String, Any]
      case (Some(e1), Some(e2)) => merge(e1, e2)
      case (Some(e1), None) => e1
      case (None, Some(e2)) => e2
      case _ => b
    }
  }

  private def toJson(obj: Any): Any = {
    import scala.collection.JavaConversions._
    obj match {
      case m: Map[_, _] =>
        val kv = for ((k, v) <- m) yield {
          s""""${k}":${toJson(v)}"""
        }
        s"{${kv.mkString(",")}}"
      case a: Seq[_] =>
        val arr = a.map(toJson(_)).mkString(",")
        s"[${arr}]"
      case a@Array(hd, tl@_*) =>
        val arr = a.map(toJson(_)).mkString(",")
        s"[${arr}]"
      case it: java.lang.Iterable[_] =>
        val arr = it.iterator().map(toJson(_)).mkString(",")
        s"[${arr}]"
      case s: String =>
        s""""$s""""
      case other =>
        other.toString
    }
  }


  post("/modify") {
    ensureLogin { u =>
      try {
        val service = params("service")
        val token = params.get("token").map(_.trim)
        val keys = params.get("keys").map(_.trim.split("/\\s+/"))
        val paramName = params.get("param_name").map(_.trim)
        val paramValue = params.get("param_value").map(_.trim)
        val selectQuery = "SELECT arg FROM subscriptions WHERE user_id=? FOR UPDATE"
        val updateQuery = "UPDATE subscriptions SET arg=? WHERE user_id=?"
        transaction { conn =>
          val argJson = conn.executeAndGet(selectQuery, u.id)(_.getString("arg")).head
          val arg = mapper.readValue[Map[String, Any]](argJson)
          val serviceProp = Map.newBuilder[String, Any]
          token.map(serviceProp += "token" -> _)
          keys.map(serviceProp += "keys" -> _)
          for (pn <- paramName; pv <- paramValue) {
            serviceProp += "params" -> Map(pn -> pv)
          }
          val updated = merge(arg, Map(service -> serviceProp.result)).asInstanceOf[Map[String, Any]]
          conn.execute(updateQuery, toJson(updated), u.id)
        }
      }
      catch {
        case e: Exception =>
          logger.error(e)
          e.printStackTrace()
      }

      redirect("/modify")
    }
  }

  private val trustManager: Array[TrustManager] = Array(
    new X509TrustManager() {
      override def getAcceptedIssuers: Array[X509Certificate] = null
      override def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}
      override def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}
    }
  )
  private val unsafeSSLSocketFactory = {
    val sc = SSLContext.getInstance("SSL")
    sc.init(null, trustManager, new SecureRandom())
    sc.getSocketFactory
  }
  private val unsafeHostnameVarifier = new HostnameVerifier {
    override def verify(s: String, sslSession: SSLSession): Boolean = true
  }

  def fetchApi(method: String, uri: String, headers: Map[String, Any], params: Map[String, Any]): Map[String, Any] = {
    val urlStr = s"${uri}?${params.map { case (k, v) => s"$k=${URLEncoder.encode(v.toString, "UTF-8")}" }.mkString("&")}"
    val c = new URI(urlStr).toURL.openConnection()
    val conn: HttpURLConnection = c match {
      case hs: HttpsURLConnection =>
        hs.setSSLSocketFactory(unsafeSSLSocketFactory)
        hs.setHostnameVerifier(unsafeHostnameVarifier)
        hs
      case h: HttpURLConnection => h
    }

    implicit val codec = Codec.UTF8
    codec.onMalformedInput(CodingErrorAction.IGNORE)
    codec.onUnmappableCharacter(CodingErrorAction.IGNORE)
    conn.setRequestMethod(method)

    headers.map { case (k, v) => conn.setRequestProperty(k, v.toString) }
    val response = withResource(new BufferedInputStream(conn.getInputStream)) { in =>
      Source.fromInputStream(in).mkString
    }
    mapper.readValue[Map[String, Any]](response)
  }

  get("/data") {
    ensureLogin { u =>
      contentType = "application/json"
      try {
        val argJson = executeQuery("SELECT arg FROM subscriptions WHERE user_id=?", u.id)(_.getString("arg"))
                      .headOption.getOrElse("{}")
        val arg = mapper.readValue[Map[String, Any]](argJson)
        val data = for ((service, confObj) <- arg) yield {
          val conf = confObj.asInstanceOf[Map[String, Any]]
          val ep: Endpoint = executeQuery("SELECT meth, token_type, token_key, uri FROM endpoints WHERE service=?", service)(new
              Endpoint(_)).head
          val headers = Map.newBuilder[String, Any]
          val params = Map.newBuilder[String, Any]
          conf.get("params").map(params ++= _.asInstanceOf[Map[String, Any]])
          val token = conf.getOrElse("token", "")
          ep.tokenType match {
            case "header" => headers += ep.tokenKey -> token
            case "param" => params += ep.tokenKey -> token
            case _ =>
          }
          val uri = conf.get("keys") match {
            case Some(l@List(_, _*)) => ep.uri.format(l: _*)
            case _ => ep.uri
          }
          Map("service" -> service, "data" -> fetchApi(ep.meth, uri, headers.result, params.result))
        }
        // data to json
        response.writer.print(toJson(data.toSeq))
      }
      catch {
        case e: Exception =>
          logger.error(e)
          response.writer.print("[]")
      }
    }
  }

  get("/initialize") {
    val ret = Shell.exec("psql -f ../sql/initialize.sql isucon5f")
    response.writer.write(ret)
  }

  private val SALT_CHARS: String = Seq('a' to 'z', 'A' to 'Z', '0' to '9').flatten.mkString
  private def generateSalt: String = {
    (0 until 32).map(i => SALT_CHARS.charAt(Random.nextInt(SALT_CHARS.size))).mkString
  }

}
