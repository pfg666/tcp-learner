package sutInterface.tcp;

public class Segment implements TCPMessage {
	public final FlagSet flags;
	public final Symbol seq;
	public final Symbol ack;

	public Segment(FlagSet flags, Symbol seq, Symbol ack) {
		super();
		this.flags = flags;
		this.seq = seq;
		this.ack = ack;
	}
	
	public String toString() {
		return flags + "(" + seq + "," + ack + ")";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ack == null) ? 0 : ack.hashCode());
		result = prime * result + ((flags == null) ? 0 : flags.hashCode());
		result = prime * result + ((seq == null) ? 0 : seq.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Segment other = (Segment) obj;
		if (ack != other.ack)
			return false;
		if (flags == null) {
			if (other.flags != null)
				return false;
		} else if (!flags.equals(other.flags))
			return false;
		if (seq != other.seq)
			return false;
		return true;
	}
}
