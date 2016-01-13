package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class Protected extends Qualified
{
        public Protected(Qualifier qualifier)
        {
                super(qualifier);

        }

        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof Protected)) {
                         return false;
                } else {
                        Protected o = (Protected)obj;
                        return qualifier().equals(o.qualifier());
                }
        }
        public int hashCode()
        {
                 return 37 * (17) + qualifier().hashCode();
        }
        public String toString()
        {
                 return "Protected(" + "qualifier: " + qualifier() + ")";
        }
}
