import org.w3c.dom.Document
import javax.xml.XMLConstants
import javax.xml.namespace.NamespaceContext

class NamespaceResolver(document: Document) : NamespaceContext {
    //Taken from: https://howtodoinjava.com/xml/xpath-namespace-resolution-example/
    //Store the source document to search the namespaces
    private val sourceDocument: Document = document

    //The lookup for the namespace uris is delegated to the stored document.
    override fun getNamespaceURI(prefix: String): String {
        return if (prefix == XMLConstants.DEFAULT_NS_PREFIX) {
            sourceDocument.lookupNamespaceURI(null)
        } else {
            sourceDocument.lookupNamespaceURI(prefix)
        }
    }

    override fun getPrefix(namespaceURI: String?): String {
        return sourceDocument.lookupPrefix(namespaceURI)
    }

    override fun getPrefixes(namespaceURI: String?): Iterator<*>? {
        return null
    }
}