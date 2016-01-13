package xsbti.api;

import java.util.Arrays;
import java.util.List;
public final class Structure extends Type
{
        public Structure(Lazy<Type[]> parents, Lazy<Definition[]> declared, Lazy<Definition[]> inherited)
        {
                this.parents = parents;
                this.declared = declared;
                this.inherited = inherited;
        }
        private final Lazy<Type[]> parents;
        private final Lazy<Definition[]> declared;
        private final Lazy<Definition[]> inherited;
        public final Type[] parents()
        {
                return parents.get();
        }
        public final Definition[] declared()
        {
                return declared.get();
        }
        public final Definition[] inherited()
        {
                return inherited.get();
        }
        public boolean equals(Object obj)
        {
                 return this == obj;  // We have lazy members, so use object identity to avoid circularity.
        }
        public int hashCode()
        {
                 return super.hashCode();
        }
        public String toString()
        {
                 return super.toString();
        }
}
