package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class Super extends PathComponent
{
        public Super(Path qualifier)
        {
                this.qualifier = qualifier;
        }
        private final Path qualifier;
        public final Path qualifier()
        {
                return qualifier;
        }
        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof Super)) {
                         return false;
                } else {
                        Super o = (Super)obj;
                        return qualifier().equals(o.qualifier());
                }
        }
        public int hashCode()
        {
                 return 37 * (17) + qualifier().hashCode();
        }
        public String toString()
        {
                 return "Super(" + "qualifier: " + qualifier() + ")";
        }
}
