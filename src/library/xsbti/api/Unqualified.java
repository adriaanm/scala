package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class Unqualified extends Qualifier
{
        public Unqualified()
        {

        }

        public boolean equals(Object obj)
        {
                 if (this == obj) {
                         return true;
                } else if (!(obj instanceof Unqualified)) {
                         return false;
                } else {
                        Unqualified o = (Unqualified)obj;
                        return true;
                }
        }
        public int hashCode()
        {
                 return 17;
        }
        public String toString()
        {
                 return "Unqualified(" + "" + ")";
        }
}
