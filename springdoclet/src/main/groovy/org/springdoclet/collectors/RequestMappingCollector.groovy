package org.springdoclet.collectors

import com.sun.javadoc.AnnotationDesc
import com.sun.javadoc.ClassDoc
import groovy.xml.MarkupBuilder
import org.springdoclet.Collector
import org.springdoclet.Annotations
import org.springdoclet.PathBuilder
import org.springdoclet.SpringDoclet
import org.springdoclet.TextUtils

@SuppressWarnings("GroovyVariableNotAssigned")
class RequestMappingCollector implements Collector {

  private static String MAPPING_TYPE = 'org.springframework.web.bind.annotation.RequestMapping'
  private static String METHOD_TYPE = 'org.springframework.web.bind.annotation.RequestMethod.'

  private mappings = []

  void processClass(ClassDoc classDoc, AnnotationDesc[] annotations) {
    def annotation = getMappingAnnotation(annotations)
    if (annotation) {
      def rootPath, defaultHttpMethods
      (rootPath, defaultHttpMethods) = getMappingElements(annotation)
      processMethods classDoc, rootPath ?: "", defaultHttpMethods ?: ['GET']
    }
    else {
      processMethods classDoc, "", ['GET']
    }
  }

  private void processMethods(classDoc, rootPath, defaultHttpMethods) {
    def methods = classDoc.methods(true)
    for (method in methods) {
      for (annotation in method.annotations()) {
        def annotationType = Annotations.getTypeName(annotation)
        if (annotationType?.startsWith(MAPPING_TYPE)) {
          processMethod classDoc, method, rootPath, defaultHttpMethods, annotation
        }
      }
    }
  }

  private def processMethod(classDoc, methodDoc, rootPath, defaultHttpMethods, annotation) {
    def (path, httpMethods) = getMappingElements(annotation)
    for (httpMethod in (httpMethods ?: defaultHttpMethods)) {
      def fullpath = makeFullPath(rootPath.toString(), path.toString())
      addMapping classDoc, methodDoc, fullpath, httpMethod
    }
  }

  private String makeFullPath(String contextPath, String methodPath) {

    String tmp = '/' + contextPath + '/' + methodPath;
    tmp = tmp.replaceAll("\"", "").replaceAll("//+", "/").replaceAll("/\$", "")

    return tmp
  }



  private def getMappingAnnotation(annotations) {
    for (annotation in annotations) {
      def annotationType = Annotations.getTypeName(annotation)
      if (annotationType?.startsWith(MAPPING_TYPE)) {
        return annotation
      }
    }
    return null
  }

  private def getMappingElements(annotation) {
    def elements = annotation.elementValues()
    def path = getElement(elements, "value") ?: ""
    def httpMethods = getElement(elements, "method")?.value()
    return [path, httpMethods]
  }

  private def getElement(elements, key) {
    for (element in elements) {
      if (element.element().name() == key) {
        return element.value()
      }
    }
    return null
  }

  private void addMapping(classDoc, methodDoc, path, httpMethod) {
    def httpMethodName = httpMethod.toString() - METHOD_TYPE
    mappings << [path: path,
            httpMethodName: httpMethodName,
            className: classDoc.qualifiedTypeName(),
            text: TextUtils.getFirstSentence(methodDoc.commentText())]
  }

  void writeOutput(MarkupBuilder builder, PathBuilder paths) {
    builder.div(id: 'request-mappings') {
      h2 'Request Mappings'
      table {
        Map<String, List<?>> sorted = sortMappings()
        tr {
          th 'Category'
          th 'Method'
          th 'URL Template'
          th 'Class'
          th 'Description'
        }
        for (e in sorted) {
          tr {
            td(colspan: 5, style: "font-weight: bold; font-size: 1.1em;",  e.key )

          }
          for (m in e.value) {
            tr {
              td ""
              td m.httpMethodName
              td m.path
              td {
                a(href: paths.buildFilePath(m.className), m.className)
              }
              td { code { mkp.yieldUnescaped(m.text ?: ' ') } }
            }
          }
        }
      }
    }
  }

  private Map<String, List<?>> sortMappings() {
    mappings.sort { it.path }
    Map<String, List<?>> out = new LinkedHashMap<String, List<?>>();

    for (m in mappings) {
      String prefix = makePrefix(m.path)
      if (!out.containsKey(prefix)) {
        out.put(prefix, new ArrayList<?>())
      }

      out.get(prefix).add(m)
    }


    return out;
  }

  String makePrefix(String path) {
    if (path == null || path.length() == 0 || path.length() == 1) {
      return "/"
    }


    String[] parts = path.split("/")
    if(parts.length < SpringDoclet.config.getUrlCategorizationLevels() + 1){
      return path
    }
    else {
      return '/' + parts[1..(SpringDoclet.config.getUrlCategorizationLevels())].join("/")
    }
  }
}
