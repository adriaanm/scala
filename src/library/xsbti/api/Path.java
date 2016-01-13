package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class Path implements java.io.Serializable
{
        public Path(PathComponent[] components)
        {
                this.components = components;
        }
        private final PathComponent[] components;
        public final PathComponent[] components()
        {
                return components;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof Path)) {
                         return false;
                } else {
                        Path o = (Path)obj;
                        return Arrays.deepEquals(components(), o.components());
                }
        }
        public int hashCode()
        {
                 return 37 * (17) + Arrays.deepHashCode(components());
        }
        public String toString()
        {
                 return "Path(" + "components: " + Arrays.toString(components()) + ")";
        }
}
