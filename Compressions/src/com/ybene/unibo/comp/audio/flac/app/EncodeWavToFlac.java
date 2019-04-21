/* 
 * FLAC library (Java)
 * 
 * Copyright (c) Project Nayuki
 * https://www.nayuki.io/page/flac-library-java
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program (see COPYING.txt and COPYING.LESSER.txt).
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.ybene.unibo.comp.audio.flac.app;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import com.ybene.unibo.comp.audio.flac.common.StreamInfo;
import com.ybene.unibo.comp.audio.flac.decode.DataFormatException;
import com.ybene.unibo.comp.audio.flac.encode.BitOutputStream;
import com.ybene.unibo.comp.audio.flac.encode.FlacEncoder;
import com.ybene.unibo.comp.audio.flac.encode.RandomAccessFileOutputStream;
import com.ybene.unibo.comp.audio.flac.encode.SubframeEncoder;

/**
 * Encodes an uncompressed PCM WAV file to a FLAC file.
 * Overwrites the output file if it already exists.
 */

public final class EncodeWavToFlac {
	
	public static void main(String[] args) throws IOException {
		
		// Time measurment - start
		Instant start = Instant.now();
	
		// Files
		File inFile  = new File("./ressources/Sounds/Beethoven-Symphony_5-1.wav");
		File outFile = new File("./ressources/Sounds/Beethoven-Symphony_5-1_encoded.flac");
		
		// Read WAV file headers and audio sample data
		int[][] samples;
		int sampleRate;
		int sampleDepth;

		// Parse and check WAV header
		try (InputStream in = new BufferedInputStream(new FileInputStream(inFile))) {
			
			// ===== "RIFF" chunk descriptor =====
			
			// Check if the "ChunkID" is "RIFF"
			// Contains the letters "RIFF" in ASCII form (0x52494646 big-endian form).
			if (!readString(in, 4).equals("RIFF")) {
				throw new DataFormatException("Invalid RIFF file header...");				
			}
			
			// SKIP -- ChunkSize
			// 4 + (8 + SubChunk1Size) + (8 + SubChunk2Size)
			// This is the size of the rest of the chunk following this number.
			// This is the size of the entire file in bytes minus 8 bytes for the two fields
			// not included in this count: ChunkID and ChunkSize.
			readLittleUint(in, 4);
			
			// Format
			// Contains the letters "WAVE" (0x57415645 big-endian form).
			if (!readString(in, 4).equals("WAVE")) {				
				throw new DataFormatException("Invalid WAV file header...");
			}
			
			// ===== "fmt" Sub chunk =====
			
			// Subchunk1ID
			// Contains the letters "fmt " (0x666d7420 big-endian form).
			if (!readString(in, 4).equals("fmt ")) {
				throw new DataFormatException("Unrecognized WAV file chunk...");				
			}
			// Subchunk1Size
			// 16 for PCM.
			// This is the size of the rest of the Subchunk which follows this number.
			if (readLittleUint(in, 4) != 16) {				
				throw new DataFormatException("Unsupported WAV file type...");
			}
			// AudioFormat
			// PCM = 1 (i.e. Linear quantization).
			// Values other than 1 indicate some form of compression.
			if (readLittleUint(in, 2) != 0x0001) {				
				throw new DataFormatException("Unsupported WAV file codec...");
			}
			
			// NumChannels
			// Mono = 1, Stereo = 2, etc.
			int numChannels = readLittleUint(in, 2);
			// Check if the number of channels fits FLAC (0 - 8)
			if (numChannels < 0 || numChannels > 8) {
				throw new RuntimeException("Too many (or few) audio channels...");				
			}
			
			// SampleRate
			// 8000, 44100, etc.
			sampleRate = readLittleUint(in, 4);
			// Check if the sample rate fits FLAC
			if (sampleRate <= 0 || sampleRate >= (1 << 20)) {
				throw new RuntimeException("Sample rate too large or invalid...");				
			}
			
			// ByteRate			writeRawSample(val >> sampleShift, out);

			// SampleRate * NumChannels * BitsPerSample / 8
			int byteRate = readLittleUint(in, 4);
			// BlockAlign
			// NumChannels * BitsPerSample / 8
			// The number of bytes for one sample including all channels.
			int blockAlign = readLittleUint(in, 2);
			// SampleDepth
			sampleDepth = readLittleUint(in, 2);
			if (sampleDepth == 0 || sampleDepth > 32 || sampleDepth % 8 != 0) {				
				throw new RuntimeException("Unsupported sample depth...");
			}
			// BitsPerSample
			// 8 bits = 8, 16 bits = 16, etc.
			int bytesPerSample = sampleDepth / 8;
			if (bytesPerSample * numChannels != blockAlign) {				
				throw new RuntimeException("Invalid block align value...");
			}
			if (bytesPerSample * numChannels * sampleRate != byteRate) {
				throw new RuntimeException("Invalid byte rate value...");				
			}
			
			// ===== "Data" Sub chunk =====
			
			// Subchunk2ID
			// Contains the letters "data" (0x64617461 big-endian form).
			if (!readString(in, 4).equals("data")) {				
				throw new DataFormatException("Unrecognized WAV file chunk...");
			}
			// Subchunk2Size
			// NumSamples * NumChannels * BitsPerSample / 8
			// This is the number of bytes in the data.
			int sampleDataLen = readLittleUint(in, 4);
			if (sampleDataLen <= 0 || sampleDataLen % (numChannels * bytesPerSample) != 0) {				
				throw new DataFormatException("Invalid length of audio sample data...");
			}
			// Number of samples, calculated from the size of the chunk, the size of a sample and the number of channels.
			int numSamples = sampleDataLen / (numChannels * bytesPerSample);
			samples = new int[numChannels][numSamples];
			
			// ===== DATA =====
			
			// Parsing the samples (data)
			for (int i = 0; i < numSamples; i++) {
				for (int ch = 0; ch < numChannels; ch++) {
					int val = readLittleUint(in, bytesPerSample);
					
					if (sampleDepth == 8) {
						val -= 128;						
					} else {						
						val = (val << (32 - sampleDepth)) >> (32 - sampleDepth);
					}
					
					samples[ch][i] = val;
				}
			}
			// Note: There might be chunks after "data", but they can be ignored.
		}
		
		// Open output file and encode samples to FLAC
		try (RandomAccessFile raf = new RandomAccessFile(outFile, "rw")) {
			// Truncate an existing file
			raf.setLength(0);
			BitOutputStream out = new BitOutputStream(new BufferedOutputStream(new RandomAccessFileOutputStream(raf)));
			out.writeInt(32, 0x664C6143);
			
			// Populate and write the stream info structure
			StreamInfo info = new StreamInfo();
			info.sampleRate = sampleRate;
			info.numChannels = samples.length;
			info.sampleDepth = sampleDepth;
			info.numSamples = samples[0].length;
			info.md5Hash = StreamInfo.getMd5Hash(samples, sampleDepth);
			info.write(true, out);
			
			// Encode all frames
			new FlacEncoder(info, samples, 4096, SubframeEncoder.SearchOptions.SUBSET_BEST, out);
			out.flush();
			
			// Rewrite the stream info metadata block, which is located at a fixed offset in the file by definition.
			raf.seek(4);
			info.write(true, out);
			out.flush();
		}
		
		// Time measurment - stop
		Instant end = Instant.now();
		// Formating and printing result
		String time = Duration.between(start, end).toString();
		time = time.substring(2);
		
		int cpt = 0;
		char[] chars = time.toCharArray();
		
		while(chars[cpt] != '.') {
			cpt++;
		}
		
		time = time.substring(0, cpt + 3) + " seconds";
		System.out.println(time);
	}
	
	// Reads len bytes from the given stream and interprets them as a UTF-8 string.
	private static String readString(InputStream in, int len) throws IOException {
		byte[] temp = new byte[len];
		
		for (int i = 0; i < temp.length; i++) {
			int b = in.read();
			
			if (b == -1) {				
				throw new EOFException();
			}
			
			temp[i] = (byte)b;
		}
		
		return new String(temp, StandardCharsets.UTF_8);
	}
	
	// Reads n bytes (0 <= n <= 4) from the given stream, interpreting
	// them as an unsigned integer encoded in little endian.
	private static int readLittleUint(InputStream in, int n) throws IOException {
		int result = 0;
		
		for (int i = 0; i < n; i++) {
			int b = in.read();
			
			if (b == -1) {				
				throw new EOFException();
			}
			
			result |= b << (i * 8);
		}
		
		return result;
	}
	
}
