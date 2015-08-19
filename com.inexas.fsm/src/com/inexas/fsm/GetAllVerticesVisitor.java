package com.inexas.fsm;

import java.util.Map;
import com.inexas.util.StrictMap;

class GetAllVerticesVisitor implements Visitor {
	public StrictMap<String, Vertex> map = new StrictMap<>();

	@Override
	public void visit(Vertex vertex) {
		if(!(vertex instanceof Fsm)) {
			map.put(vertex.getFullName(), vertex);
		}
	}

	@Override
	public void visit(Transition transition) {
		// ignore me
	}

	public Map<String, Vertex> getMap() {
		map.lock();
		return map;
	}

}
