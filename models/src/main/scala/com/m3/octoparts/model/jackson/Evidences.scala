package com.m3.octoparts.model.jackson

import com.m3.octoparts.model.config.ParamType
import com.m3.octoparts.model.HttpMethod
import com.fasterxml.jackson.core.`type`.TypeReference

class ParamTypeType extends TypeReference[ParamType.type]
class HttpMethodType extends TypeReference[HttpMethod.type]

