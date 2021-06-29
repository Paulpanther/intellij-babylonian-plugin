package de.hpi.swa.liveprogramming.types;

import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.tools.utils.json.JSONException;
import com.oracle.truffle.tools.utils.json.JSONObject;

import java.util.Objects;

public final class FilePos {
	private final int line;
	private final int start;
	private final int end;

	public static FilePos fromJSON(JSONObject json) {
		try {
			return new FilePos(
					json.getInt("line") + 1,
					JSON.orDefault(json, "start", 0, JSONObject::getInt),
					JSON.orDefault(json, "end", 0, JSONObject::getInt));
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static FilePos fromSourceSection(SourceSection section) {
		return new FilePos(section.getStartLine(), section.getStartColumn(), section.getEndColumn() - 1);
	}

	public FilePos(int line, int start, int end) {
		this.line = line;
		this.start = start;
		this.end = end;
	}

	public FilePos(int line) {
		this.line = line;
		this.start = 0;
		this.end = 0;
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("line", line - 1);
		json.put("start", start);
		json.put("end", end);
		return json;
	}

	public SourceSectionFilter.IndexRange getLineRange() {
		return SourceSectionFilter.IndexRange.between(line, line + 1);
	}

	public SourceSectionFilter.IndexRange getColumnRange() {
		return SourceSectionFilter.IndexRange.between(start, end + 1);
	}

	public int getLine() {
		return line;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FilePos filePos = (FilePos) o;
		return line == filePos.line && start == filePos.start && end == filePos.end;
	}

	@Override
	public int hashCode() {
		return Objects.hash(line, start, end);
	}
}
