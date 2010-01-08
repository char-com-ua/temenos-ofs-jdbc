package org.t24.driver;

import java.util.ArrayList;



class ParamList<E> extends ArrayList<E> {
	public ParamList(){
		super();
	}
	
	
	public E set(int index, E element) {
		if(index<0 || index>1000)throw new IndexOutOfBoundsException( "ParamList Index out of bound: "+index);

		//increase 
		this.ensureCapacity(index+1);
        for (int i = this.size(); i <= index; i++) {
            this.add(null);
        }
        return super.set(index,element);
	}
	
	
}
