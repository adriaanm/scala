package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class _internalOnly_NameHash implements java.io.Serializable
{
        public _internalOnly_NameHash(String name, int hash)
        {
                this.name = name;
                this.hash = hash;
        }
        private final String name;
        private final int hash;
        public final String name()
        {
                return name;
        }
        public final int hash()
        {
                return hash;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof _internalOnly_NameHash)) {
                         return false;
                } else {
                        _internalOnly_NameHash o = (_internalOnly_NameHash)obj;
                        return name().equals(o.name()) && hash() == o.hash();
                }
        }
        public int hashCode()
        {
                 return 37 * (37 * (17) + name().hashCode()) + hash();
        }
        public String toString()
        {
                 return "_internalOnly_NameHash(" + "name: " + name() + ", " + "hash: " + hash() + ")";
        }
}
