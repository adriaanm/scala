package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class Private extends Qualified
{
        public Private(Qualifier qualifier)
        {
                super(qualifier);

        }

        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof Private)) {
                         return false;
                } else {
                        Private o = (Private)obj;
                        return qualifier().equals(o.qualifier());
                }
        }
        public int hashCode()
        {
                 return 37 * (17) + qualifier().hashCode();
        }
        public String toString()
        {
                 return "Private(" + "qualifier: " + qualifier() + ")";
        }
}
