package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class Singleton extends SimpleType
{
        public Singleton(Path path)
        {
                this.path = path;
        }
        private final Path path;
        public final Path path()
        {
                return path;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof Singleton)) {
                         return false;
                } else {
                        Singleton o = (Singleton)obj;
                        return path().equals(o.path());
                }
        }
        public int hashCode()
        {
                 return 37 * (17) + path().hashCode();
        }
        public String toString()
        {
                 return "Singleton(" + "path: " + path() + ")";
        }
}
