package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class Id extends PathComponent
{
        public Id(String id)
        {
                this.id = id;
        }
        private final String id;
        public final String id()
        {
                return id;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof Id)) {
                         return false;
                } else {
                        Id o = (Id)obj;
                        return id().equals(o.id());
                }
        }
        public int hashCode()
        {
                 return 37 * (17) + id().hashCode();
        }
        public String toString()
        {
                 return "Id(" + "id: " + id() + ")";
        }
}
