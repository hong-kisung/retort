import retort.utils.logging.Logger
import retort.exception.RetortException
import static retort.utils.Utils.delegateParameters as getParam

/**
 * kubectl apply
 *
 * @param file resource file. YAML or json
 * @param folder resource folder.
 * @param namespace namespace
 * @param recoverOnFail Delete resource when fail applying.
 * @param option apply option
 * @param wait : 300 . Wait n seconds while this resource rolled out
 */
def apply(ret) {
  Logger logger = Logger.getLogger(this)
  def config = getParam(ret, [wait: 300, recoverOnFail: false])
  
  def command = new StringBuffer('kubectl apply')
  if (config.file) {
    logger.debug("FILE : ${config.file}")
    command.append(" -f ${config.file}")
  } else if (config.folder) {
    logger.debug("FOLDER : ${config.file}")
    command.append(" -f ${config.file}")
  } else {
    throw createException('RC301')   
  }
  
  if (config.option) {
    logger.debug("OPTION : ${config.option}")
    command.append(" ${config.option}")
  }
  
  if (config.namespace) {
    logger.debug("NAMESPACE : ${config.namespace}")
    command.append(" -n ${config.namespace}")
  }
  
  command.append(" --record=true")

  if (config.file) {
    executeApplyFile(command, config, logger)
  } else if (config.folder) {
    executeApplyFolder(command, config, logger)
  }

}

/**
 * kubectl describe
 *
 * @param type resource type. ex) deploy, service etc.
 * @param name resource name or name prefix
 * @param label label selector array
 * @param file resource yaml file
 * @param namespace namespace
 * @param throwException : false throw Exception 
 */
def describe(ret) {
  Logger logger = Logger.getLogger(this)
  def config = getParam(ret, [throwException: false])
  
  def command = new StringBuffer('kubectl describe')
  
  if (config.type) {
    logger.debug("RESOURCE TYPE : ${config.type}")
    command.append(" ${config.type}")
    
    if (config.name) {
      // kubectl describe type name
      logger.debug("RESOURCE NAME : ${config.name}")
      command.append(" ${config.name}")
    } else if (config.label) {
      if ((config.label instanceof List) || config.label.getClass().isArray()) {
        // kubectl describe type [-l key=value ]+
        command.append config.label.collect{ l ->
            logger.debug("LABEL-SELECTOR : ${l}")
            return " -l ${l}"
          }.join()
      } else if (config.label instanceof Map) {
        command.append config.label.collect { k, v ->
            logger.debug("LABEL-SELECTOR : ${k}=${v}")
            return " -l ${k}=${v}"
          }.join()
      } else {
        logger.error('describe : label only support Map, List, Array type parameter.')
        if (config.throwException == true) {
          throw createException('RC311')
        }
      }
    } else {
      logger.error('describe : type value should be used with name or label.')
      if (config.throwException == true) {
        throw createException('RC302')
      }
    }
  } else if (config.file) {
    logger.debug("RESOURCE FILE : ${config.file}")
    command.append(" -f ${config.file}")
  } else {
    logger.error('describe : type and name values are required. or specify file value.')
    if (config.throwException == true) {
      throw createException('RC303')
    }
    return
  }
  
  if (config.namespace) {
    logger.debug("NAMESPACE : ${config.namespace}")
    command.append(" -n ${config.namespace}")
  }
  
  try {
    sh command.toString()
  } catch (Exception e) {
    logger.error('Exception occured while running describe command : ${command.toString()}')
    if (config.throwException == true) {
      throw createException('RC306', e, command.toString())
    }
  }

}

/**
 * Check resource exists.
 *
 * @param type resource type. ex) deploy, service etc.
 * @param name resource name or name prefix
 * @param label label selector array
 * @param file resource yaml file
 * @param namespace namespace
 * @param throwException : false throw Exception
 * @return boolean resource exists or not. if error occured and throwException : false return false.
 */
def resourceExists(ret) {
  Logger logger = Logger.getLogger(this)
  def config = getParam(ret, [throwException: false])
  
  def command = new StringBuffer('kubectl get')
  def result = false
  if (config.type) {
    logger.debug("RESOURCE TYPE : ${config.type}")
    command.append(" ${config.type}")
    
    if (config.name) {
      // kubectl describe type name
      logger.debug("RESOURCE NAME : ${config.name}")
      command.append(" ${config.name}")
    } else if (config.label) {
      if ((config.label instanceof List) || config.label.getClass().isArray()) {
        // kubectl describe type [-l key=value ]+
        command.append config.label.collect{ l ->
            logger.debug("LABEL-SELECTOR : ${l}")
            return " -l ${l}"
          }.join()
      } else if (config.label instanceof Map) {
        command.append config.label.collect { k, v ->
            logger.debug("LABEL-SELECTOR : ${k}=${v}")
            return " -l ${k}=${v}"
          }.join()
      } else {
        logger.error('resourceExists : label only support Map, List, Array type parameter.')
        if (config.throwException == true) {
          throw createException('RC311')
        }
      }
    } else {
      logger.error('resourceExists : type value should be used with name or label.')
      if (config.throwException == true) {
        throw createException('RC302')
      }
      return result
    }
  } else if (config.file) {
    logger.debug("RESOURCE FILE : ${config.file}")
    command.append(" -f ${config.file}")
  } else {
    logger.error('resourceExists : type and name values are required. or specify file value.')
    if (config.throwException == true) {
      throw createException('RC303')
    }
    return result
  }
  
  if (config.namespace) {
    logger.debug("NAMESPACE : ${config.namespace}")
    command.append(" -n ${config.namespace}")
  }
  
  try {
    def status = sh script: "${command.toString()}", returnStatus: true
    result = status == 0 ? true : false
  } catch (Exception e) {
    logger.error('Exception occured while checking resource is exist : ${command.toString()}')
    if (config.throwException == true) {
      throw createException('RC309', e, command.toString())
    }
  }
  
  return result
}

/**
 * kubectl get with json path
 *
 * @param type
 * @param name
 * @param file
 * @param jsonpath 
 * @param namespace
 * @param throwException : false throw Exception 
 * @return String value
 */
def getValue(ret) {
  Logger logger = Logger.getLogger(this)
  def config = getParam(ret, [throwException: false])
  
  def value = ''
  def command = new StringBuffer('kubectl get')
  
  if (config.type && config.name) {
    logger.debug("RESOURCE TYPE : ${config.type}")
    command.append(" ${config.type}")
    
    // kubectl get type name
    logger.debug("RESOURCE NAME : ${config.name}")
    command.append(" ${config.name}")
  } else if (config.file) {
    logger.debug("RESOURCE FILE : ${config.file}")
    command.append(" -f ${config.file}")
  } else {
    logger.error('getValue : type and name values are required. or specify file value.')
    if (config.throwException == true) {
      throw createException('RC303')
    }
    return value
  }
  
  if (config.namespace) {
    logger.debug("NAMESPACE : ${config.namespace}")
    command.append(" -n ${config.namespace}")
  }
  
  if (config.jsonpath) {
    logger.debug("JSONPATH : ${config.jsonpath}")
    command.append(" -o jsonpath=${config.jsonpath}")
  } else {
    logger.error('jsonpath value is required.')
    if (config.throwException == true) {
      throw createException('RC304')
    }
    return value
  }
  
  try {
    value = sh script: command.toString(), returnStdout: true
  } catch (Exception e) {
    logger.error("Exception occured while getting jsonpath : ${config.jsonpath}")
    if (config.throwException == true) {
      throw createException('RC305', e, config.jsonpath)
    }
  }
  
  return value
}

/**
 * kubectl rollout status
 *
 * @param type
 * @param name
 * @param file
 * @param namespace
 * @param wait : 300
 * @param throwException : false throw Exception 
 */
def rolloutStatus(ret) {
  Logger logger = Logger.getLogger(this)
  def config = getParam(ret, [wait: 300])
  
  try {
      
    if (!(config.wait instanceof Integer)) {
      logger.error("wait value must be Integer but received ${config.wait.getClass().toString()}")
      throw createException('RC313', config.wait.getClass().toString())
    }
    
    def command = new StringBuffer('kubectl rollout status')
    
    if (config.type && config.name) {
      logger.debug("RESOURCE TYPE : ${config.type}")
      command.append(" ${config.type}")
      
      // kubectl get type name
      logger.debug("RESOURCE NAME : ${config.name}")
      command.append(" ${config.name}")
    } else if (config.file) {
      logger.debug("RESOURCE FILE : ${config.file}")
      command.append(" -f ${config.file}")
    } else {
      logger.error('rolloutStatus : type and name values are required. or specify file value.')
      throw createException('RC303')
    }
    
    if (config.namespace) {
      logger.debug("NAMESPACE : ${config.namespace}")
      command.append(" -n ${config.namespace}")
    }
    
    def config2 = config.clone()
    def resourceKind
    def resourceName
    try {
      config2.put('jsonpath', '{.kind}/{.metadata.name}')
      def resource = getValue config2
      
      resourceKind = resource.tokenize('/')[0]
      resourceName = resource.tokenize('/')[1]
    } catch (Exception e2) {
      logger.error("Resource does not exists. Can not execute rollout status.")
      throw createException('RC316')
    }
    
    def rolloutPossibleResources = ['deployment', 'daemonset', 'statefullset']
    if (!rolloutPossibleResources.contains(resourceKind.toLowerCase())) {
      throw createException('RC317', resourceKind)
    }

    
    try {
      logger.debug("Waiting for ${config.wait} seconds, during ${resourceKind}/${resourceName} being applied.")
      timeout (time: config.wait, unit: 'SECONDS') {
        try {
          sh command.toString()
        } catch (Exception e) {
          // https://wiki.jenkins.io/display/JENKINS/Job+Exit+Status
          // You sent it a signal with the UNIX kill command, or SGE command qdel. 
          // If you don't specify which signal to send, kill defaults to SIGTERM (exit code 15+128=143) 
          // and qdel sends SIGINT (exit code 2+128=130), then SIGTERM, then SIGKILL until your job dies.
          // TIMEOUT 
          if (e.getMessage().contains('143')) {
            logger.error('Timeout occured')
            throw e
          } else {
            logger.error("Exception occured while running rollout status : ${resourceKind}/${resourceName}")
            throw createException('RC307', e, config.jsonpath)
          }
  
        }
      }
    } catch (Exception e) {
      logger.error('/////////////')
      // sh fail
      if (e instanceof RetortException && e.getErrorCode() == 'RC307') {
        throw e
      }
  
      // timeout
      logger.error("Timeout occured while ${resource} being applied. Check events.")
      
      config2.put('throwException', false)
      describe config2
      throw createException('RC308', e, resource)
    }
  } catch (Exception e) {
    if (config.throwException == true) {
      throw e
    }

  }

}


/**
 * kubectl rollout undo
 *
 * @param type
 * @param name
 * @param file
 * @param revision
 * @param namespace
 * @param wait : 300
 */
def rolloutUndo(ret) {
  Logger logger = Logger.getLogger(this)
  def config = getParam(ret, [wait: 300])
  
  if (!(config.wait instanceof Integer)) {
    logger.error("wait value must be Integer but received ${config.wait.getClass().toString()}")
    throw createException('RC313', config.wait.getClass().toString())
  }
  
  def command = new StringBuffer('kubectl rollout undo')
  
  if (config.type && config.name) {
    logger.debug("RESOURCE TYPE : ${config.type}")
    command.append(" ${config.type}")
    
    // kubectl get type name
    logger.debug("RESOURCE NAME : ${config.name}")
    command.append(" ${config.name}")
  } else if (config.file) {
    logger.debug("RESOURCE FILE : ${config.file}")
    command.append(" -f ${config.file}")
  } else {
    logger.error('rolloutUndo : type and name values are required. or specify file value.')
    throw createException('RC303')
  }
  
  if (config.namespace) {
    logger.debug("NAMESPACE : ${config.namespace}")
    command.append(" -n ${config.namespace}")
  }
  
  if (config.revision) {
    logger.debug("TO REVISION : ${config.revision}")
    command.append(" --to-revision=${config.revision}")
  }
  
  def config2 = config.clone()
  def resourceKind
  def resourceName
  try {
    config2.put('jsonpath', '{.kind}/{.metadata.name}')
    def resource = getValue config2
    
    resourceKind = resource.tokenize('/')[0]
    resourceName = resource.tokenize('/')[1]
  } catch (Exception e2) {
    logger.error("Resource does not exists. Can not execute rollout status.")
    throw createException('RC316')
  }
  
  def rolloutPossibleResources = ['deployment', 'daemonset', 'statefullset']
  if (!rolloutPossibleResources.contains(resourceKind.toLowerCase())) {
    throw createException('RC317', resourceKind)
  }
  
  try {
    sh command.toString()
  } catch (Exception e) {
    logger.error("Exception occured while running rollout undo command : ${command.toString()}")
    throw createException('RC314', e, command.toString())
  }
  
  if (config.wait > 0) {
    rolloutStatus config
  }
  
}

/**
 * kubectl delete
 *
 * @param type
 * @param name
 * @param file
 * @param namespace
 * @param force
 */
def delete(ret) {
  Logger logger = Logger.getLogger(this)
  def config = getParam(ret)
  
  def command = new StringBuffer('kubectl delete')
  
  if (config.type && config.name) {
    logger.debug("RESOURCE TYPE : ${config.type}")
    command.append(" ${config.type}")
    
    // kubectl get type name
    logger.debug("RESOURCE NAME : ${config.name}")
    command.append(" ${config.name}")
  } else if (config.file) {
    logger.debug("RESOURCE FILE : ${config.file}")
    command.append(" -f ${config.file}")
  } else {
    logger.error('delete : type and name values are required. or specify file value.')
    throw createException('RC303')
  }
  
  if (config.namespace) {
    logger.debug("NAMESPACE : ${config.namespace}")
    command.append(" -n ${config.namespace}")
  }
  
  if (config.force == true) {
    logger.debug("FORCE : ${config.force}")
    command.append(" -force ${config.force}")
  }
  
  def config2 = config.clone()
  config2.put('jsonpath', '{.kind}/{.metadata.name}')
  def resource
  try {
    resource = getValue config2
  } catch (Exception e2) {
    if (config.type && config.name) {
      resource = "${config.type}/${config.name}"
    } else {
      resource = config.file
    }
  }
  
  try {
    sh command.toString()
  } catch (Exception e) {
    logger.error("Exception occured while running rollout undo command : ${command.toString()}")
    throw createException('RC315', e, command.toString())
  }
  
}


/**
 * excute apply command with file.
 */
private def executeApplyFile(command, config, logger) {
  if (!(config.wait instanceof Integer)) {
    logger.error('wait value must be Integer but received ${config.wait.getClass().toString()}')
    throw createException('RC313', config.wait.getClass().toString())
  }

  def exists = false
  try {
    exists = resourceExists config
    
    // no need to recover
    sh command.toString()

    // rollout
    if (config.wait > 0) {
      def config2 = config.clone()
      config2.put('throwException', true)
      rolloutStatus config
    }

    
  } catch (Exception e) {
    if (e instanceof RetortException) {
      logger.error('Exception occured while waiting rollout.')
      if (config.recoverOnFail) {
        recoverApply(exists, config, logger)
      }
      throw createException('RC312', e, config.file)
      
    } else {
      logger.error('Exception occured while applying.')
      throw createException('RC310', e, config.file)
    }

  }

}

/**
 * 
 */
private def recoverApply(exists, config, logger) {
  if (exists) {
     // rollback
     rolloutUndo config
  } else {
    // delete        
    delete config
  }
}

/**
 * excute apply command with folder.
 */
private def executeApplyFolder(command, config, logger) {
  try {
    // no need to recover
    sh command.toString()
  } catch (Exception e) {
    logger.error("Exception occured while applying : ${config.folder}")
    throw createException('RC310', e, config.folder)
  }
}
