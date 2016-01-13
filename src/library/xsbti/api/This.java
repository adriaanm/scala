package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class This extends PathComponent
{
        public This()
        {

        }

        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof This)) {
                         return false;
                } else {
                        This o = (This)obj;
                        return true;
                }
        }
        public int hashCode()
        {
                 return 17;
        }
        public String toString()
        {
                 return "This(" + "" + ")";
        }
}
