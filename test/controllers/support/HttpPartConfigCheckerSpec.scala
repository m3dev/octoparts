package controllers.support

import com.m3.octoparts.model.config.ParamType
import com.m3.octoparts.support.PlayAppSupport
import com.m3.octoparts.support.mocks.ConfigDataMocks
import org.scalatest.{ FunSpec, Matchers }
import play.api.i18n._

import scala.collection.SortedSet

class HttpPartConfigCheckerSpec extends FunSpec with Matchers with ConfigDataMocks with PlayAppSupport {

  private implicit val messages = MessagesImpl(Lang("en"), new DefaultMessagesApi())

  describe("QueryParamInterpolation") {
    it("Should be ok") {
      QueryParamInterpolation.warnings(mockHttpPartConfig.copy(uriToInterpolate = "http://test/${test}")) shouldBe 'empty
    }
    it("Should have warnings") {
      QueryParamInterpolation.warnings(mockHttpPartConfig.copy(uriToInterpolate = "http://test?test=${test}")) should have size 1
    }
  }

  describe("MissingPathParam") {
    it("Should be ok") {
      MissingPathParam.warnings(mockHttpPartConfig.copy(uriToInterpolate = "http://test/${test}", parameters = SortedSet(mockPartParam.copy(paramType = ParamType.Path, outputName = "test")))) shouldBe 'empty
    }
    it("Should have warnings") {
      val w = MissingPathParam.warnings(mockHttpPartConfig.copy(uriToInterpolate = "http://test/${test}", parameters = SortedSet.empty))
      w should have size 1
      w.head should include("'test'")
    }
  }

  describe("PathParamNoInterp") {
    it("Should be ok") {
      PathParamNoInterp.warnings(mockHttpPartConfig.copy(uriToInterpolate = "http://test/${test}", parameters = SortedSet(mockPartParam.copy(paramType = ParamType.Path, outputName = "test")))) shouldBe 'empty
    }
    it("Should have warnings") {
      val w = PathParamNoInterp.warnings(mockHttpPartConfig.copy(uriToInterpolate = "http://test/", parameters = SortedSet(mockPartParam.copy(paramType = ParamType.Path, outputName = "test"))))
      w should have size 1
      w.head should include("'test'")
    }
  }

  describe("PathParamOption") {
    it("Should be ok") {
      PathParamOption.warnings(mockHttpPartConfig.copy(parameters = SortedSet(mockPartParam.copy(paramType = ParamType.Path, required = true)))) shouldBe 'empty
    }
    it("Should have warnings") {
      val w = PathParamOption.warnings(mockHttpPartConfig.copy(parameters = SortedSet(mockPartParam.copy(paramType = ParamType.Path, required = false, outputName = "test"))))
      w should have size 1
      w.head should include("'test'")
    }
  }

  describe("WhitespaceInUri") {
    it("Should be ok") {
      WhitespaceInUri.warnings(mockHttpPartConfig.copy(uriToInterpolate = "http://test")) shouldBe 'empty
    }
    it("Should have warnings") {
      WhitespaceInUri.warnings(mockHttpPartConfig.copy(uriToInterpolate = "http://test ")) should have size 1
      WhitespaceInUri.warnings(mockHttpPartConfig.copy(uriToInterpolate = "http://test/ /")) should have size 1
    }
  }

  describe("AlertEmailOff") {
    it("Should be ok") {
      AlertEmailOff.warnings(mockHttpPartConfig.copy(alertMailsEnabled = true)) shouldBe 'empty
    }
    it("Should have warnings") {
      AlertEmailOff.warnings(mockHttpPartConfig.copy(alertMailsEnabled = false)) should have size 1
    }
  }
}
