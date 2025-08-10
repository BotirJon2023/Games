import React, { forwardRef, useEffect, useRef } from 'react';

interface DartBoardProps {
  className?: string;
}

const DartBoard = forwardRef<HTMLCanvasElement, DartBoardProps>(
  ({ className = '' }, ref) => {
    const internalRef = useRef<HTMLCanvasElement>(null);
    const canvasRef = (ref as React.RefObject<HTMLCanvasElement>) || internalRef;

    useEffect(() => {
      const canvas = canvasRef.current;
      if (!canvas) return;

      const ctx = canvas.getContext('2d');
      if (!ctx) return;

      drawDartboard(ctx, canvas.width, canvas.height);
    }, []);

    const drawDartboard = (ctx: CanvasRenderingContext2D, width: number, height: number) => {
      const centerX = width / 2;
      const centerY = height / 2;
      const radius = Math.min(width, height) / 2 - 20;

      // Clear canvas
      ctx.clearRect(0, 0, width, height);

      // Background
      ctx.fillStyle = '#1a1a1a';
      ctx.fillRect(0, 0, width, height);

      // Dartboard outer ring
      ctx.beginPath();
      ctx.arc(centerX, centerY, radius, 0, 2 * Math.PI);
      ctx.fillStyle = '#2d5016';
      ctx.fill();
      ctx.strokeStyle = '#000';
      ctx.lineWidth = 3;
      ctx.stroke();

      // Numbers around the dartboard
      const numbers = [20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5];
      
      // Draw sections
      for (let i = 0; i < 20; i++) {
        const angle1 = (i * 18 - 9) * Math.PI / 180;
        const angle2 = ((i + 1) * 18 - 9) * Math.PI / 180;
        
        // Determine colors for alternating sections
        const isEven = i % 2 === 0;
        const outerColor = isEven ? '#f4f4f4' : '#1a1a1a';
        const innerColor = isEven ? '#dc2626' : '#16a34a';

        // Outer single area
        drawSection(ctx, centerX, centerY, radius * 0.6, radius, angle1, angle2, outerColor);
        
        // Triple ring
        drawSection(ctx, centerX, centerY, radius * 0.55, radius * 0.6, angle1, angle2, innerColor);
        
        // Inner single area
        drawSection(ctx, centerX, centerY, radius * 0.3, radius * 0.55, angle1, angle2, outerColor);
        
        // Double ring
        drawSection(ctx, centerX, centerY, radius * 0.25, radius * 0.3, angle1, angle2, innerColor);

        // Draw number
        const numberAngle = i * 18 * Math.PI / 180;
        const numberX = centerX + Math.cos(numberAngle - Math.PI / 2) * (radius + 15);
        const numberY = centerY + Math.sin(numberAngle - Math.PI / 2) * (radius + 15);
        
        ctx.fillStyle = '#fff';
        ctx.font = 'bold 16px Arial';
        ctx.textAlign = 'center';
        ctx.fillText(numbers[i].toString(), numberX, numberY + 5);
      }

      // Bull's eye (outer)
      ctx.beginPath();
      ctx.arc(centerX, centerY, radius * 0.08, 0, 2 * Math.PI);
      ctx.fillStyle = '#16a34a';
      ctx.fill();
      ctx.strokeStyle = '#000';
      ctx.lineWidth = 2;
      ctx.stroke();

      // Bull's eye (inner)
      ctx.beginPath();
      ctx.arc(centerX, centerY, radius * 0.04, 0, 2 * Math.PI);
      ctx.fillStyle = '#dc2626';
      ctx.fill();
      ctx.stroke();

      // Sector lines
      for (let i = 0; i < 20; i++) {
        const angle = (i * 18 - 9) * Math.PI / 180;
        const x1 = centerX + Math.cos(angle) * radius * 0.25;
        const y1 = centerY + Math.sin(angle) * radius * 0.25;
        const x2 = centerX + Math.cos(angle) * radius;
        const y2 = centerY + Math.sin(angle) * radius;
        
        ctx.beginPath();
        ctx.moveTo(x1, y1);
        ctx.lineTo(x2, y2);
        ctx.strokeStyle = '#000';
        ctx.lineWidth = 2;
        ctx.stroke();
      }

      // Ring separators
      const rings = [0.25, 0.3, 0.55, 0.6];
      rings.forEach(ringRadius => {
        ctx.beginPath();
        ctx.arc(centerX, centerY, radius * ringRadius, 0, 2 * Math.PI);
        ctx.strokeStyle = '#000';
        ctx.lineWidth = 2;
        ctx.stroke();
      });
    };

    const drawSection = (
      ctx: CanvasRenderingContext2D,
      centerX: number,
      centerY: number,
      innerRadius: number,
      outerRadius: number,
      startAngle: number,
      endAngle: number,
      color: string
    ) => {
      ctx.beginPath();
      ctx.arc(centerX, centerY, outerRadius, startAngle, endAngle);
      ctx.arc(centerX, centerY, innerRadius, endAngle, startAngle, true);
      ctx.closePath();
      ctx.fillStyle = color;
      ctx.fill();
      ctx.strokeStyle = '#000';
      ctx.lineWidth = 1;
      ctx.stroke();
    };

    return (
      <div className={`flex justify-center ${className}`}>
        <canvas
          ref={canvasRef}
          width={400}
          height={400}
          className="border-2 border-yellow-600 rounded-lg shadow-2xl bg-green-900"
        />
      </div>
    );
  }
);

DartBoard.displayName = 'DartBoard';

export default DartBoard;