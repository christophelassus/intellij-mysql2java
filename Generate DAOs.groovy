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
        (~/(?i)tinyint/)                            : "boolean",
        (~/(?i)int/)                                : "long",
        (~/(?i)float|double|decimal|real/)          : "double",
        (~/(?i)datetime|timestamp|date|time/)       : "Date",
        (~/(?i)/)                                   : "String"
]


rsGetTypeMapping = [
        (~/(?i)int/)                                : "Int",
        (~/(?i)float|double|decimal|real/)          : "Double",
        (~/(?i)datetime|timestamp|time/)            : "Timestamp",
        (~/(?i)date/)                               : "Date",
        (~/(?i)/)                                   : "String"
]


FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
    SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
    def className = javaName(table.getName(), true)
    def fields = calcFields(table)
    new File(dir, className + ".java").withPrintWriter { out -> generate(out, className, fields) }
}

/*

    private static Item mapResultSet(ResultSet rs) throws SQLException
    {
        Item item = new Item();

        item.setId(rs.getLong("id"));
        item.setName(rs.getString("name"));
        item.setCoeffMarginX100(rs.getInt("coeff_margin_x100"));
        if(rs.wasNull()) item.setCoeffMarginX100(null);
        item.setUpdatedAt(rs.getTimestamp("updated_at"));

        return item;
    }
 */




def generate(out, className, fields) {
    out.println "package $packageName"
    out.println ""
    out.println ""
    out.println "public class ${className}Dao {"
    out.println ""

    out.println "\tprivate static ${className} mapResultSet(ResultSet rs) throws SQLException"
    out.println "\t{"
    out.println "\t\t${className} obj = new ${className}(); "

    fields.each() {
        out.println "\t\tobj.set${it.name.capitalize()}(rs.get${it.typeRsGet}(\"${it.name}\"));"
        if(it.isNull) out.println "\t\tif(rs.wasNull()) obj.set${it.name.capitalize()}(null);"
    }

    out.println "\t\treturn obj; "
    out.println "\t}"

    out.println "}"
}

def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDasType().getSpecification())
        def isNull = !col.isNotNull();
        def typeRsGetStr = rsGetTypeMapping.find { p, t -> p.matcher(spec).find() }.value
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        if(isNull) typeStr = typeStr.capitalize()
        fields += [[
                           name : javaName(col.getName(), false),
                           type : typeStr,
                           typeRsGet : typeRsGetStr,
                           isNull: isNull,
                           annos: ""]]
    }
}

def javaName(str, capitalize) {
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1? s : Case.LOWER.apply(s[0]) + s[1..-1]
}
