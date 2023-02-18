import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

packageName = "com.sample;"
typeMapping = [
        (~/(?i)tinyint/)                     : "boolean",
        (~/(?i)bigint/)                      : "long",
        (~/(?i)int/)                         : "int",
        (~/(?i)float|double|decimal|real/)   : "double",
        (~/(?i)datetime|timestamp|date|time/): "Date",
        (~/(?i)/)                            : "String"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
    SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
    def className = javaName(table.getName(), true)
    if(className.endsWith("ies")) className = className.substring(0,className.length()-3)+"y" //replace final "ies" with "y"
    else if(className.endsWith("ses")) className = className.substring(0,className.length()-3)+"sis" //replace final ses by sis analysis -> analyses
    else if(className.endsWith("oes")) className = className.substring(0,className.length()-2) //shoes -> shoe papatoes -> patato
    else if(className.endsWith("ves")) className = className.substring(0,className.length()-3)+"f" //wife->wives, wolf->wolves
    else if(className.endsWith("es")) className = className.substring(0,className.length()-2) //remove final es class-> classes
    else if(className.endsWith("s")) className = className.substring(0,className.length()-1) //remove final s

    def fields = calcFields(table)
    new File(dir, className + ".java").withPrintWriter { out -> generate(out, className, fields, dir) }
}

def generate(out, className, fields,dir) {

    def packageName = workoutPackageName(dir)
    out.println "package ${packageName};"
    out.println ""
    out.println ""
    out.println "public class $className {"
    out.println ""
    fields.each() {
        if (it.annos != "") out.println "  ${it.annos}"
        out.println "  private ${it.type} ${it.name};"
    }

    out.println "";

    generateDefaultConstructor(out, className, fields)

    out.println ""

    generateCopyConstructor(out, className, fields);

    out.println ""


    fields.each() {
        out.println ""
        out.println "  public ${it.type} get${it.name.capitalize()}() {"
        out.println "    return ${it.name};"
        out.println "  }"
        out.println ""
        out.println "  public void set${it.name.capitalize()}(${it.type} ${it.name}) {"
        out.println "    this.${it.name} = ${it.name};"
        out.println "  }"
        out.println ""
    }
    out.println "}"
}

def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDasType().getSpecification())
        def isNull = !col.isNotNull();
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        if (isNull) typeStr = typeStr.capitalize()
        if("Int".equals(typeStr)) typeStr = "Integer"
        fields += [[
                           name : javaName(col.getName(), false),
                           type : typeStr,
                           annos: ""]]
    }
}

def javaName(str, capitalize) {
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}


def generateCopyConstructor(out, className, fields) {

    out.println " public ${className}(${className} obj)"
    out.println " {"
    fields.each() {
        out.println "  this.${it.name}=obj.${it.name};"
    }
    out.println " }"

}

def generateDefaultConstructor(out, className, fields) {

    out.println " public ${className}()"
    out.println " {"
    out.println " }"

}

def workoutPackageName(dir)
{
    def dirName = ""+dir;
    def prefix = "/src/"
    if(dirName.indexOf(prefix)>-1)
    {
        def startIndex = dirName.indexOf(prefix) + prefix.length()
        def packageName = dirName.substring(startIndex)
        return packageName.replaceAll(/\//,".")
    }
    else return dirName;
}