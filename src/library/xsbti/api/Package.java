package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class Package implements java.io.Serializable
{
        public Package(String name)
        {
                this.name = name;
        }
        private final String name;
        public final String name()
        {
                return name;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof Package)) {
                         return false;
                } else {
                        Package o = (Package)obj;
                        return name().equals(o.name());
                }
        }
        public int hashCode()
        {
                 return 37 * (17) + name().hashCode();
        }
        public String toString()
        {
                 return "Package(" + "name: " + name() + ")";
        }
}
