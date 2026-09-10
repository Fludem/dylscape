import java.util.List;

/** Prints `.local/component.sym` lines for a ComponentBuilder exposing `componentNames`. */
public class NamesDump {
    public static void main(String[] args) throws Exception {
        Class<?> c = Class.forName(args[0]);
        Object b = c.getField("INSTANCE").get(null);
        String iface = (String) c.getField("INTERFACE").get(null);
        @SuppressWarnings("unchecked")
        List<String> names = (List<String>) c.getMethod("getComponentNames").invoke(b);
        for (int i = 0; i < names.size(); i++) {
            System.out.println(iface + ":" + i + "\t" + iface + ":" + names.get(i));
        }
    }
}
